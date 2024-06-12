package no.nav.etterlatte.joarkhendelser.oppgave

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
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
    ) {
        logger.info("Oppretter manuell journalføringsoppgave")

        val response =
            httpClient
                .post("$url/api/v1/oppgaver") {
                    contentType(ContentType.Application.Json)
                    setBody(OpprettOppgaveRequest(journalpostId.toString(), tema))
                }.body<JsonNode>()

        logger.info("Opprettet oppgave (id=${response["id"]}, status=${response["status"]}, tema=${response["tema"]})")
        sikkerlogger().info("Opprettet oppgave med respons: \n$response")
    }
}

@Suppress("unused")
data class OpprettOppgaveRequest(
    val journalpostId: String,
    val tema: String,
) {
    val aktoerId: String? = null
    val oppgavetype = "JFR" // JFR = Journalføring
    val prioritet = "NORM"
    val aktivDato = LocalDate.now().toString()
    val fristFerdigstillelse = LocalDate.now().plusDays(30).toString()
    val beskrivelse = "Journalpost $journalpostId mangler bruker"
}
