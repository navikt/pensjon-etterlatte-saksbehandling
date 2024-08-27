package no.nav.etterlatte.inntektsjustering

import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.gyldigsoeknad.journalfoering.AvsenderMottaker
import no.nav.etterlatte.gyldigsoeknad.journalfoering.Bruker
import no.nav.etterlatte.gyldigsoeknad.journalfoering.DokarkivKlient
import no.nav.etterlatte.gyldigsoeknad.journalfoering.DokumentVariant
import no.nav.etterlatte.gyldigsoeknad.journalfoering.JournalpostDokument
import no.nav.etterlatte.gyldigsoeknad.journalfoering.JournalpostSak
import no.nav.etterlatte.gyldigsoeknad.journalfoering.OpprettJournalpostRequest
import no.nav.etterlatte.gyldigsoeknad.journalfoering.OpprettJournalpostResponse
import no.nav.etterlatte.gyldigsoeknad.pdf.PdfGenerator
import no.nav.etterlatte.libs.common.RetryResult
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.inntektsjustering.Inntektsjustering
import no.nav.etterlatte.libs.common.retry
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.toJsonNode
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Base64
import java.util.UUID

class JournalfoerInntektsjusteringService(
    private val dokarkivKlient: DokarkivKlient,
    private val pdfgenKlient: PdfGenerator,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private val encoder = Base64.getEncoder()

    fun opprettJournalpost(
        sak: Sak,
        inntektsjustering: Inntektsjustering,
    ): OpprettJournalpostResponse? {
        return try {
            val tittel = "Inntektsjustering ${inntektsjustering.inntektsaar}"
            val dokument = opprettDokument(tittel, sak.id, inntektsjustering)

            val request =
                OpprettJournalpostRequest(
                    tittel = tittel,
                    tema = sak.sakType.tema,
                    journalfoerendeEnhet = sak.enhet,
                    avsenderMottaker = AvsenderMottaker(sak.ident),
                    bruker = Bruker(sak.ident),
                    eksternReferanseId = opprettEksternReferanseId(inntektsjustering.id, sak.sakType),
                    sak = JournalpostSak(sak.id.toString()),
                    dokumenter = listOf(dokument),
                )

            runBlocking { dokarkivKlient.opprettJournalpost(request) }
        } catch (e: Exception) {
            logger.error("Feil oppsto ved journalføring av inntektsjustering (id=${inntektsjustering.id})", e)
            return null
        }
    }

    private fun opprettDokument(
        tittel: String,
        sakId: Long,
        inntektsjustering: Inntektsjustering,
    ): JournalpostDokument {
        try {
            logger.info("Oppretter PDF for inntektsjustering (id=${inntektsjustering.id})")

            val pdf = opprettArkivPdf(sakId, inntektsjustering)

            return JournalpostDokument(
                tittel = tittel,
                dokumentvarianter = listOf(pdf),
            )
        } catch (e: ResponseException) {
            throw Exception("Klarte ikke å generere PDF for inntektsjustering med id=${inntektsjustering.id}", e)
        }
    }

    private fun opprettEksternReferanseId(
        id: UUID,
        sakType: SakType,
    ): String = "etterlatte:${sakType.name.lowercase()}:inntektsjustering:$id"

    private fun opprettArkivPdf(
        sakId: Long,
        inntektsjustering: Inntektsjustering,
    ): DokumentVariant.ArkivPDF {
        logger.info("Oppretter arkiv PDF for inntektsjustering med id ${inntektsjustering.id}")

        return runBlocking {
            retry {
                pdfgenKlient.genererPdf(
                    input =
                        ArkiverInntektsjustering(
                            id = inntektsjustering.id,
                            sakId = sakId,
                            aar = inntektsjustering.inntektsaar,
                            arbeidsinntekt = inntektsjustering.arbeidsinntekt,
                            naeringsinntekt = inntektsjustering.naeringsinntekt,
                            arbeidsinntektUtland = inntektsjustering.arbeidsinntektUtland,
                            naeringsinntektUtland = inntektsjustering.naeringsinntektUtland,
                            tidspunkt = inntektsjustering.formatertTidspunkt(),
                        ).toJsonNode(),
                    template = "inntektsjustering_nytt_aar_v1",
                )
            }.let {
                when (it) {
                    is RetryResult.Success -> DokumentVariant.ArkivPDF(encoder.encodeToString(it.content))
                    is RetryResult.Failure -> {
                        logger.error("Kunne ikke opprette PDF for inntektsjustering med id ${inntektsjustering.id}")
                        throw it.samlaExceptions()
                    }
                }
            }
        }
    }
}

data class ArkiverInntektsjustering(
    val id: UUID,
    val sakId: Long,
    val aar: Int,
    val arbeidsinntekt: Int,
    val naeringsinntekt: Int,
    val arbeidsinntektUtland: Int,
    val naeringsinntektUtland: Int,
    val tidspunkt: String,
)

fun Inntektsjustering.formatertTidspunkt(): String {
    fun t(tall: Int) = if (tall < 10) "0$tall" else "$tall"
    return with(LocalDateTime.ofInstant(tidspunkt, ZoneOffset.ofHours(0))) {
        "${t(dayOfMonth)}.${t(monthValue)}.$year ${t(hour)}:${t(minute)}:${t(second)}"
    }
}
