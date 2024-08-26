package no.nav.etterlatte

import com.fasterxml.jackson.databind.JsonNode
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
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.retry
import no.nav.etterlatte.libs.common.sak.Sak
import org.slf4j.LoggerFactory
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
        inntektsaar: String,
        inntektsjustering: Inntektsjustering,
    ): OpprettJournalpostResponse? {
        return try {
            val tittel = "Inntektsjustering $inntektsaar"
            val dokument = opprettDokument(tittel, sak.id, inntektsaar, inntektsjustering)

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
        sakId: no.nav.etterlatte.libs.common.sak.SakId,
        inntektsaar: String,
        inntektsjustering: Inntektsjustering,
    ): JournalpostDokument {
        try {
            logger.info("Oppretter PDF for inntektsjustering (id=${inntektsjustering.id})")

            val pdf = opprettArkivPdf(sakId, inntektsaar, inntektsjustering)

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
        sakId: no.nav.etterlatte.libs.common.sak.SakId,
        inntektsaar: String,
        inntektsjustering: Inntektsjustering,
    ): DokumentVariant.ArkivPDF {
        logger.info("Oppretter arkiv PDF for inntektsjustering med id ${inntektsjustering.id}")

        return runBlocking {
            retry {
                pdfgenKlient.genererPdf(jsonInnhold(sakId, inntektsaar, inntektsjustering), "tom_mal")
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

// TODO skal erstattes med egen mal
private fun jsonInnhold(
    sakId: no.nav.etterlatte.libs.common.sak.SakId,
    aar: String,
    inntektsjustering: Inntektsjustering,
): JsonNode {
    val json = """
    {
      "tittel" : "Inntektsjustering $aar",
      "payload" : [
        {
          "type" : "heading-three",
          "children" : [
            {
              "type" : "paragraph",
              "text" : "Inntekt for sak $sakId ble mottatt ${inntektsjustering.tidspunkt}"
            }
          ]
        },
        {
          "type" : "paragraph",
          "children" : [
            {
              "type" : "paragraph",
              "text" : "Arbeidsinntekt: ${inntektsjustering.arbeidsinntekt}"
            },
            {
              "type" : "paragraph",
              "text" : "Næringsinntekt: ${inntektsjustering.naeringsinntekt}"
            },
            {
              "type" : "paragraph",
              "text" : "Arbeidsinntekt utland: ${inntektsjustering.arbeidsinntektUtland}"
            },
            {
              "type" : "paragraph",
              "text" : "Næringsinntekt utland: ${inntektsjustering.naeringsinntektUtland}"
            }
          ]
        }
      ]
    }
"""
    return objectMapper.readTree(json)
}
