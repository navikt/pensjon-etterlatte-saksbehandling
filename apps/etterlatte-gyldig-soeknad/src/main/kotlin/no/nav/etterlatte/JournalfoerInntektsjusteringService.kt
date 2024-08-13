package no.nav.etterlatte

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
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
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.toJsonNode
import org.slf4j.LoggerFactory
import java.util.Base64

class JournalfoerInntektsjusteringService(
    private val dokarkivKlient: DokarkivKlient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private val encoder = Base64.getEncoder()

    fun opprettJournalpost(
        // id: Long, TODO
        sak: Sak,
        inntektsjustering: String,
    ): OpprettJournalpostResponse? {
        return try {
            val tittel = "Inntektsjustering"
            val dokument = opprettDokument(tittel, inntektsjustering)

            val request =
                OpprettJournalpostRequest(
                    tittel = tittel,
                    tema = sak.sakType.tema,
                    journalfoerendeEnhet = sak.enhet,
                    avsenderMottaker = AvsenderMottaker(sak.ident),
                    bruker = Bruker(sak.ident),
                    eksternReferanseId = opprettEksternReferanseId(123L, sak.sakType), // TODO
                    sak = JournalpostSak(sak.id.toString()),
                    dokumenter = listOf(dokument),
                )

            runBlocking { dokarkivKlient.opprettJournalpost(request) }
        } catch (e: Exception) {
            logger.error("Feil oppsto ved journalføring av søknad (id=$)", e) // TODO
            return null
        }
    }

    private fun opprettDokument(
        tittel: String,
        inntektsjustering: String,
    ): JournalpostDokument {
        try {
            logger.info("Oppretter original JSON for inntektsjustering (id=)") // TODO

            val originalJson = opprettOriginalJson(inntektsjustering)

            return JournalpostDokument(
                tittel = tittel,
                dokumentvarianter = listOf(originalJson),
            )
        } catch (e: ResponseException) {
            throw Exception("Klarte ikke å generere PDF for søknad med id=", e) // TODO
        }
    }

    private fun opprettOriginalJson(inntektsjustering: String): DokumentVariant.OriginalJson {
        // logger.info("Oppretter original JSON for søknad (id=$soeknadId)")

        val bytes = jacksonObjectMapper().writeValueAsBytes(inntektsjustering.toJsonNode())

        return DokumentVariant.OriginalJson(encoder.encodeToString(bytes))
    }

    private fun opprettEksternReferanseId(
        id: Long,
        sakType: SakType,
    ): String = "etterlatte:${sakType.name.lowercase()}:$id"
}
