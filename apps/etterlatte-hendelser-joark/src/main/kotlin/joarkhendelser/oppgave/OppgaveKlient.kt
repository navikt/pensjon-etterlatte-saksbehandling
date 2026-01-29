package no.nav.etterlatte.joarkhendelser.oppgave

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.joarkhendelser.KjenteSkjemaKoder
import no.nav.etterlatte.joarkhendelser.joark.BrukerIdType
import no.nav.etterlatte.joarkhendelser.joark.Journalpost
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import org.slf4j.LoggerFactory
import java.time.LocalDate

class OppgaveKlient(
    private val httpClient: HttpClient,
    private val url: String,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun opprettManuellJournalfoeringsoppgave(
        journalpostId: Long,
        tema: String,
        tildeltEnhetsnr: String? = null,
    ) {
        logger.info("Oppretter manuell journalføringsoppgave")

        val response =
            httpClient
                .post("$url/api/v1/oppgaver") {
                    contentType(ContentType.Application.Json)
                    setBody(OpprettOppgaveRequest.journalpostManglerBruker(journalpostId.toString(), tema, tildeltEnhetsnr))
                }.body<JsonNode>()

        logger.info("Opprettet oppgave (id=${response["id"]}, status=${response["status"]}, tema=${response["tema"]})")
        sikkerlogger().info("Opprettet oppgave med respons: \n$response")
    }

    suspend fun opprettOppgaveForKabal(
        journalpost: Journalpost,
        tema: String,
        kjentSkjemaKode: KjenteSkjemaKoder,
    ) {
        val opprettOppgaveRequest = OpprettOppgaveRequest.journalpostSkalTilKabal(journalpost, tema, kjentSkjemaKode)
        try {
            val response =
                httpClient
                    .post("$url/api/v1/oppgaver") {
                        contentType(ContentType.Application.Json)
                        setBody(opprettOppgaveRequest)
                    }.body<JsonNode>()
            logger.info("Opprettet oppgave for kabal (id=${response["id"]}, status=${response["status"]}, tema=${response["tema"]})")
            sikkerlogger().info("Opprettet oppgave for kabal med respons", kv("response", response))
        } catch (e: Exception) {
            logger.error(
                "Kunne ikke opprette oppgave for journalpost med id=${journalpost.journalpostId} til kabal, på grunn av feil. Se sikkerlogg for detaljer.",
            )
            sikkerlogger().error("Kunne ikke opprette oppgave for journalpost med id=${journalpost.journalpostId}", e)
        }
    }
}

@Suppress("unused")
data class OpprettOppgaveRequest private constructor(
    val journalpostId: String,
    val tema: String,
    val aktoerId: String?,
    val prioritet: String,
    val fristFerdigstillelse: String?,
    val beskrivelse: String,
    val personident: String?,
    val orgnr: String?,
    val tildeltEnhetsnr: String?,
) {
    // JFR = Journalføring
    val oppgavetype: String = "JFR"
    val aktivDato: String = LocalDate.now().toString()

    companion object {
        fun journalpostManglerBruker(
            journalpostId: String,
            tema: String,
            tildeltEnhetsnr: String?,
        ): OpprettOppgaveRequest =
            OpprettOppgaveRequest(
                journalpostId = journalpostId,
                tema = tema,
                aktoerId = null,
                prioritet = "NORM",
                fristFerdigstillelse = LocalDate.now().plusDays(30).toString(),
                beskrivelse = "Journalpost $journalpostId mangler bruker",
                personident = null,
                orgnr = null,
                tildeltEnhetsnr = tildeltEnhetsnr,
            )

        fun journalpostSkalTilKabal(
            journalpost: Journalpost,
            tema: String,
            kjentSkjemaKode: KjenteSkjemaKoder,
        ): OpprettOppgaveRequest {
            val (aktoerId, personident, orgnr) = utledMottaker(journalpost)

            return OpprettOppgaveRequest(
                journalpostId = journalpost.journalpostId,
                tema = tema,
                aktoerId = aktoerId,
                prioritet = "NORM",
                fristFerdigstillelse = null,
                beskrivelse = "Innsendt ${kjentSkjemaKode.navn} for tema $tema",
                personident = personident,
                orgnr = orgnr,
                tildeltEnhetsnr = Enheter.KLAGE_VEST.enhetNr.enhetNr,
            )
        }

        private fun utledMottaker(journalpost: Journalpost): OpprettOppgaveRequestBruker {
            if (journalpost.bruker == null) {
                return OpprettOppgaveRequestBruker()
            }
            return when (journalpost.bruker.type) {
                BrukerIdType.AKTOERID -> OpprettOppgaveRequestBruker(aktoerId = journalpost.bruker.id)
                BrukerIdType.FNR -> OpprettOppgaveRequestBruker(personident = journalpost.bruker.id)
                BrukerIdType.ORGNR -> OpprettOppgaveRequestBruker(orgnr = journalpost.bruker.id)
            }
        }

        private data class OpprettOppgaveRequestBruker(
            val aktoerId: String? = null,
            val personident: String? = null,
            val orgnr: String? = null,
        )
    }
}
