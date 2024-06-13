package no.nav.etterlatte.gyldigsoeknad.journalfoering

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.gyldigsoeknad.pdf.PdfGenerator
import no.nav.etterlatte.libs.common.RetryResult
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.InnsendtSoeknad
import no.nav.etterlatte.libs.common.retry
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.toJsonNode
import org.slf4j.LoggerFactory
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
    ): OpprettJournalpostResponse? {
        return try {
            val tittel = opprettTittel(sak.sakType)
            val dokument = opprettDokument(soeknadId, tittel, soeknad, soeknad.template())

            val request =
                OpprettJournalpostRequest(
                    tittel = tittel,
                    tema = sak.sakType.tema,
                    journalfoerendeEnhet = sak.enhet,
                    avsenderMottaker = AvsenderMottaker(sak.ident),
                    bruker = Bruker(sak.ident),
                    eksternReferanseId = opprettEksternReferanseId(soeknadId, sak.sakType),
                    sak = JournalpostSak(sak.id.toString()),
                    dokumenter = listOf(dokument),
                )

            runBlocking { dokarkivKlient.opprettJournalpost(request) }
        } catch (e: Exception) {
            logger.error("Feil oppsto ved journalføring av søknad (id=$soeknadId)", e)
            return null
        }
    }

    fun opprettJournalpostForUkjent(
        soeknadId: Long,
        sakType: SakType,
        soeknad: InnsendtSoeknad,
    ): OpprettJournalpostResponse? =
        try {
            val tittel = opprettTittel(sakType)
            val dokument = opprettDokument(soeknadId, tittel, soeknad, soeknad.template())

            val request =
                OpprettJournalpostRequest(
                    tittel = tittel,
                    tema = sakType.tema,
                    journalfoerendeEnhet = Enheter.defaultEnhet.enhetNr,
                    avsenderMottaker = null,
                    bruker = null,
                    eksternReferanseId = opprettEksternReferanseId(soeknadId, sakType),
                    sak = null,
                    dokumenter = listOf(dokument),
                )

            runBlocking { dokarkivKlient.opprettJournalpost(request, forsoekFerdistill = false) }
        } catch (e: Exception) {
            logger.error("Feil oppsto ved journalføring av søknad (id=$soeknadId)", e)
            null
        }

    private fun opprettTittel(sakType: SakType) =
        when (sakType) {
            SakType.BARNEPENSJON -> "Søknad om barnepensjon"
            SakType.OMSTILLINGSSTOENAD -> "Søknad om omstillingsstønad"
        }

    private fun opprettEksternReferanseId(
        soeknadId: Long,
        sakType: SakType,
    ): String = "etterlatte:${sakType.name.lowercase()}:$soeknadId"

    private fun opprettDokument(
        soeknadId: Long,
        tittel: String,
        soeknad: InnsendtSoeknad,
        template: String,
    ): JournalpostDokument {
        try {
            val arkivPdf = opprettArkivPdf(soeknadId, soeknad, template)

            logger.info("Oppretter original JSON for søknad (id=$soeknadId)")
            val originalJson = opprettOriginalJson(soeknadId, soeknad)

            return JournalpostDokument(
                tittel = tittel,
                dokumentvarianter = listOf(arkivPdf, originalJson),
            )
        } catch (e: ResponseException) {
            throw Exception("Klarte ikke å generere PDF for søknad med id=$soeknadId", e)
        }
    }

    private fun opprettOriginalJson(
        soeknadId: Long,
        soeknad: InnsendtSoeknad,
    ): DokumentVariant.OriginalJson {
        logger.info("Oppretter original JSON for søknad (id=$soeknadId)")

        val skjemaInfoBytes = jacksonObjectMapper().writeValueAsBytes(soeknad.toJsonNode())

        return DokumentVariant.OriginalJson(encoder.encodeToString(skjemaInfoBytes))
    }

    private fun opprettArkivPdf(
        soeknadId: Long,
        soeknad: InnsendtSoeknad,
        template: String,
    ): DokumentVariant.ArkivPDF {
        logger.info("Oppretter arkiv PDF for søknad med id $soeknadId")

        return runBlocking {
            retry {
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
