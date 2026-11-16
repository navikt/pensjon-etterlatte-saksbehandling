package no.nav.etterlatte.brukerdialog.soeknad.journalfoering

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.jackson3.jackson
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.common.objectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

internal class DokarkivKlientTest {
    private val request =
        OpprettJournalpostRequest(
            tittel = "Søknad om utvidet omstillingsstønad",
            tema = "EYO",
            journalfoerendeEnhet = null,
            avsenderMottaker = null,
            bruker = null,
            eksternReferanseId = "etterlatte:omstillingsstoenad:43",
            sak = JournalpostSak("123"),
            dokumenter = emptyList(),
        )

    @ParameterizedTest
    @ValueSource(ints = [201, 409])
    fun `opprettet og allerede journalfoert gir samme journalpost`(status: Int) =
        runBlocking {
            HttpClient(
                MockEngine {
                    assertEquals(HttpMethod.Post, it.method)
                    assertEquals("true", it.url.parameters["forsoekFerdigstill"])
                    val body = objectMapper.readTree(it.body.toByteArray())
                    assertEquals(request.eksternReferanseId, body["eksternReferanseId"].asText())
                    respond(
                        content = """{"journalpostId":"456","journalpostferdigstilt":true}""",
                        status = HttpStatusCode.fromValue(status),
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                },
            ) {
                install(ContentNegotiation) { jackson {} }
            }.use { httpClient ->
                val klient = DokarkivKlient(client = httpClient, url = "http://dokarkiv/journalpost")

                assertEquals(
                    OpprettJournalpostResponse(journalpostId = "456", journalpostferdigstilt = true),
                    klient.opprettJournalpost(request),
                )
            }
        }

    @Test
    fun `journalfoering paa ukjent forsoeker ikke aa ferdigstille`() =
        runBlocking {
            HttpClient(
                MockEngine {
                    assertEquals("false", it.url.parameters["forsoekFerdigstill"])
                    respond(
                        content = """{"journalpostId":"456","journalpostferdigstilt":false}""",
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                },
            ) {
                install(ContentNegotiation) { jackson {} }
            }.use { httpClient ->
                val klient = DokarkivKlient(client = httpClient, url = "http://dokarkiv/journalpost")

                assertEquals(
                    OpprettJournalpostResponse(journalpostId = "456", journalpostferdigstilt = false),
                    klient.opprettJournalpost(request = request.copy(sak = null), forsoekFerdigstill = false),
                )
            }
        }

    @Test
    fun `feil fra dokarkiv kastes videre`() {
        HttpClient(MockEngine { respond("Dokarkiv er utilgjengelig", HttpStatusCode.ServiceUnavailable) }) {
            install(ContentNegotiation) { jackson {} }
        }.use { httpClient ->
            val klient = DokarkivKlient(client = httpClient, url = "http://dokarkiv/journalpost")

            assertThrows(ForespoerselException::class.java) {
                runBlocking { klient.opprettJournalpost(request) }
            }
        }
    }
}
