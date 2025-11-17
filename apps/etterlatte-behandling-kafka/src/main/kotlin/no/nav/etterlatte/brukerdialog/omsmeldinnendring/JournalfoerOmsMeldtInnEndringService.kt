package no.nav.etterlatte.brukerdialog.omsmeldinnendring

import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brukerdialog.formatertTidspunkt
import no.nav.etterlatte.brukerdialog.soeknad.journalfoering.AvsenderMottaker
import no.nav.etterlatte.brukerdialog.soeknad.journalfoering.Bruker
import no.nav.etterlatte.brukerdialog.soeknad.journalfoering.DokarkivKlient
import no.nav.etterlatte.brukerdialog.soeknad.journalfoering.DokumentVariant
import no.nav.etterlatte.brukerdialog.soeknad.journalfoering.JournalpostDokument
import no.nav.etterlatte.brukerdialog.soeknad.journalfoering.JournalpostSak
import no.nav.etterlatte.brukerdialog.soeknad.journalfoering.OpprettJournalpostRequest
import no.nav.etterlatte.brukerdialog.soeknad.journalfoering.OpprettJournalpostResponse
import no.nav.etterlatte.brukerdialog.soeknad.pdf.PdfGeneratorKlient
import no.nav.etterlatte.libs.common.RetryResult
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.PDFMal
import no.nav.etterlatte.libs.common.omsmeldinnendring.ForventetInntektTilNesteAar
import no.nav.etterlatte.libs.common.omsmeldinnendring.OmsMeldtInnEndring
import no.nav.etterlatte.libs.common.retry
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import org.slf4j.LoggerFactory
import java.util.Base64
import java.util.UUID

class JournalfoerOmsMeldtInnEndringService(
    private val dokarkivKlient: DokarkivKlient,
    private val pdfgenKlient: PdfGeneratorKlient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private val encoder = Base64.getEncoder()

    fun opprettJournalpost(
        sak: Sak,
        omsMeldtInnEndring: OmsMeldtInnEndring,
    ): OpprettJournalpostResponse? {
        return try {
            val tittel = "Meldt inn endring Omstillingstønad"
            val dokument = opprettDokument(tittel, sak.id, omsMeldtInnEndring)

            val request =
                OpprettJournalpostRequest(
                    tittel = tittel,
                    tema = sak.sakType.tema,
                    journalfoerendeEnhet = sak.enhet,
                    avsenderMottaker = AvsenderMottaker(sak.ident),
                    bruker = Bruker(sak.ident),
                    eksternReferanseId = "etterlatte:${sak.sakType.name.lowercase()}:omsMeldtInnEndring:${omsMeldtInnEndring.id}",
                    sak = JournalpostSak(sak.id.toString()),
                    dokumenter = listOf(dokument),
                )

            runBlocking { dokarkivKlient.opprettJournalpost(request) }
        } catch (e: Exception) {
            logger.error(
                "Feil oppsto ved journalføring av meldt inn endring for Omstillingstønad (id=${sak.id})",
                e,
            )
            return null
        }
    }

    private fun opprettDokument(
        tittel: String,
        sakId: SakId,
        omsMeldtInnEndring: OmsMeldtInnEndring,
    ): JournalpostDokument {
        try {
            logger.info("Oppretter PDF for meldt inn endring for Omstillingstønad (id=$sakId)")

            val pdf = opprettArkivPdf(sakId, omsMeldtInnEndring)

            return JournalpostDokument(
                tittel = tittel,
                dokumentvarianter = listOf(pdf),
            )
        } catch (e: ResponseException) {
            throw Exception("Klarte ikke å generere PDF for meldt inn endring for Omstillingstønad med id=$sakId", e)
        }
    }

    private fun opprettArkivPdf(
        sakId: SakId,
        omsMeldtInnEndring: OmsMeldtInnEndring,
    ): DokumentVariant {
        logger.info("Oppretter arkiv PDF for meldt inn endring for Omstillingstønad med id $sakId")

        return runBlocking {
            retry {
                pdfgenKlient.genererPdf(
                    payload =
                        ArkiverOmsMeldtInnEndring(
                            id = omsMeldtInnEndring.id,
                            sakId = sakId,
                            type = omsMeldtInnEndring.endring.name,
                            endringer = omsMeldtInnEndring.beskrivelse,
                            tidspunkt = formatertTidspunkt(omsMeldtInnEndring.tidspunkt),
                            forventetInntektTilNesteAar = omsMeldtInnEndring.forventetInntektTilNesteAar,
                        ),
                    mal = "oms_meldt_inn_endring_v1",
                )
            }.let {
                when (it) {
                    is RetryResult.Success -> DokumentVariant.ArkivPDF(encoder.encodeToString(it.content))
                    is RetryResult.Failure -> {
                        logger.error("Kunne ikke opprette PDF for meldt inn endring for Omstilingstønad med id $sakId")
                        throw it.samlaExceptions()
                    }
                }
            }
        }
    }
}

data class ArkiverOmsMeldtInnEndring(
    val id: UUID,
    val sakId: SakId,
    val type: String,
    val endringer: String,
    val tidspunkt: String,
    val forventetInntektTilNesteAar: ForventetInntektTilNesteAar?,
) : PDFMal
