package no.nav.etterlatte.gyldigsoeknad.journalfoering

import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.RetryResult
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.InnsendtSoeknad
import no.nav.etterlatte.libs.common.retry
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.toJsonNode
import org.slf4j.LoggerFactory
import pdf.PdfGenerator
import java.util.Base64

class JournalfoerSoeknadService(
    private val dokarkivKlient: DokarkivKlient,
    private val pdfgenKlient: PdfGenerator,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private val encoder = Base64.getEncoder()

    fun opprettJournalpost(
        soeknadId: Long,
        sak: Sak,
        soeknad: InnsendtSoeknad,
    ): OpprettJournalpostResponse {
        val tittel =
            when (sak.sakType) {
                SakType.BARNEPENSJON -> "Søknad om barnepensjon"
                SakType.OMSTILLINGSSTOENAD -> "Søknad om omstillingsstønad"
            }

        val dokument = opprettDokument(soeknadId, tittel, soeknad, soeknad.template())

        val request =
            OpprettJournalpostRequest(
                tittel = tittel,
                tema = sak.sakType.tema,
                journalfoerendeEnhet = sak.enhet,
                avsenderMottaker = AvsenderMottaker(sak.ident),
                bruker = Bruker(sak.ident),
                eksternReferanseId = "etterlatte:${sak.sakType.toString().lowercase()}:$soeknadId",
                sak = JournalpostSak(sak.id.toString()),
                dokumenter = listOf(dokument),
            )

        return runBlocking {
            dokarkivKlient.opprettJournalpost(request)
        }
    }

    private fun opprettDokument(
        soeknadId: Long,
        tittel: String,
        soeknad: InnsendtSoeknad,
        template: String,
    ): JournalpostDokument {
        try {
            val arkivPdf = opprettArkivPdf(soeknadId, soeknad, template)

            logger.info("Oppretter original JSON for søknad med id $soeknadId")
            val originalJson = DokumentVariant.OriginalJson(soeknad.toJson())

            return JournalpostDokument(
                tittel = tittel,
                dokumentvarianter = listOf(arkivPdf, originalJson),
            )
        } catch (e: ResponseException) {
            throw Exception("Klarte ikke å generere PDF for søknad med id=$soeknadId", e)
        }
    }

    private fun opprettArkivPdf(
        soeknadId: Long,
        soeknad: InnsendtSoeknad,
        template: String,
    ): DokumentVariant.ArkivPDF {
        logger.info("Oppretter arkiv PDF for søknad med id $soeknadId")

        return runBlocking {
            retry<ByteArray> {
                pdfgenKlient.genererPdf(soeknad.toJsonNode(), template)
            }.let {
                when (it) {
                    is RetryResult.Success -> DokumentVariant.ArkivPDF(encoder.encodeToString(it.content))
                    is RetryResult.Failure -> {
                        logger.error("Kunne ikke opprette PDF for søknad med id=$soeknadId")
                        throw it.samlaExceptions()
                    }
                }
            }
        }
    }
}
