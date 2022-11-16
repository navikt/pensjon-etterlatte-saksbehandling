import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import io.ktor.serialization.jackson.jackson
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.distribusjon.DistribusjonException
import no.nav.etterlatte.distribusjon.DistribusjonKlient
import no.nav.etterlatte.libs.common.distribusjon.DistribuerJournalpostRequest
import no.nav.etterlatte.libs.common.distribusjon.DistribuerJournalpostResponse
import no.nav.etterlatte.libs.common.distribusjon.DistribusjonsTidspunktType
import no.nav.etterlatte.libs.common.distribusjon.DistribusjonsType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class DistribusjonKlientTest {
    private val mockResponse = DistribuerJournalpostResponse("1234")
    private val responseJson = jacksonObjectMapper().writeValueAsString(mockResponse)

    private val defaultHeaders = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))

    @Test
    fun distribuerJournalpost() {
        val distribuerJournalpostRequest = DistribuerJournalpostRequest(
            journalpostId = "abc123",
            bestillendeFagsystem = "EY",
            dokumentProdApp = "ABC",
            distribusjonstype = DistribusjonsType.VEDTAK,
            distribusjonstidspunkt = DistribusjonsTidspunktType.KJERNETID
        )

        val klient = mockHttpClient("/distribuerjournalpost") {
            respond(
                responseJson, HttpStatusCode.OK, defaultHeaders
            )
        }

        val response = runBlocking {
            klient.distribuerJournalpost(distribuerJournalpostRequest)
        }

        Assertions.assertEquals(mockResponse, response)
    }

    @Test
    fun distribuerJournalpost_alleredeDistribuert() {
        val distribuerJournalpostRequest = DistribuerJournalpostRequest(
            journalpostId = "abc123",
            bestillendeFagsystem = "EY",
            dokumentProdApp = "ABC",
            distribusjonstype = DistribusjonsType.VEDTAK,
            distribusjonstidspunkt = DistribusjonsTidspunktType.KJERNETID
        )

        val klient = mockHttpClient("/distribuerjournalpost") {
            respond(
                responseJson, HttpStatusCode.Conflict, defaultHeaders
            )
        }

        val response = runBlocking {
            klient.distribuerJournalpost(distribuerJournalpostRequest)
        }

        Assertions.assertEquals(mockResponse, response)
    }

    @Test
    fun distribuerJournalpost_feilVedKall() {
        val distribuerJournalpostRequest = DistribuerJournalpostRequest(
            journalpostId = "abc123",
            bestillendeFagsystem = "EY",
            dokumentProdApp = "ABC",
            distribusjonstype = DistribusjonsType.VEDTAK,
            distribusjonstidspunkt = DistribusjonsTidspunktType.KJERNETID
        )

        val klient = mockHttpClient("/distribuerjournalpost") {
            respond(
                "", HttpStatusCode.BadRequest, defaultHeaders
            )
        }

        assertThrows<DistribusjonException> {
            runBlocking {
                klient.distribuerJournalpost(distribuerJournalpostRequest)
            }
        }
    }

    private fun mockHttpClient(
        path: String, respond: MockRequestHandleScope.() -> HttpResponseData
    ): DistribusjonKlient {
        val httpClient = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    when (request.url.fullPath) {
                        path -> respond()
                        else -> error("Unhandled ${request.url.fullPath}")
                    }
                }
            }
            install(ContentNegotiation) { jackson { } }
        }

        return DistribusjonKlient(httpClient, "")
    }

}
