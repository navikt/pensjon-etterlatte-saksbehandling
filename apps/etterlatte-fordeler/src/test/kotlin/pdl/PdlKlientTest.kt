package no.nav.etterlatte.prosess.pdl

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.features.defaultRequest
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.logging.CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.X_CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.HentPersonRequest
import no.nav.etterlatte.pdl.PdlKlient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class PdlKlientTest {

    private lateinit var pdlKlient: PdlKlient

    private companion object {
        const val TESTUSER_FNR = "11057523044"
    }

    fun mockEndpoint(file: String) {
        val httpClient = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    val path = "/person"
                    if (request.url.fullPath == path && request.method == HttpMethod.Post
                    ) {
                        val headers = headersOf(
                            "Content-Type" to listOf(ContentType.Application.Json.toString()),
                        )
                        val payload = javaClass.getResource(file)!!.readText()
                        respond(payload, headers = headers)
                    } else {
                        error(request.url.fullPath)
                    }
                }
            }
            install(JsonFeature) { serializer = JacksonSerializer() }
        }

        pdlKlient = PdlKlient(httpClient, "/person")
    }

    @Test
    fun `skal hente person og mappe riktig til datamodell`() {
        mockEndpoint("/pdl/person.json")

        val person = runBlocking {
            pdlKlient.hentPerson(HentPersonRequest(
                foedselsnummer = Foedselsnummer.of(TESTUSER_FNR),
                historikk = true,
                adresse = true,
                utland = true,
                familieRelasjon = true
            ))
        }

        assertNotNull(person)
        assertEquals(person.fornavn, "Jens")
        assertEquals(person.etternavn, "Johnsen")
    }
}