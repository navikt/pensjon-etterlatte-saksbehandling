package no.nav.etterlatte.pdltjenester

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import io.ktor.serialization.jackson.JacksonConverter
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.FNR_1
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.HentPersonRequest
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.readFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class PdlTjenesterKlientTest {

    private lateinit var pdlTjenesterKlient: PdlTjenesterKlient

    private fun mockEndpoint(file: String) {
        val httpClient = HttpClient(MockEngine) {
            expectSuccess = true
            engine {
                addHandler { request ->
                    if (request.url.fullPath == "/person" && request.method == HttpMethod.Post) {
                        val headers = headersOf(
                            "Content-Type" to listOf(ContentType.Application.Json.toString())
                        )
                        respond(content = readFile(file), headers = headers)
                    } else {
                        error(request.url.fullPath)
                    }
                }
            }
            install(ContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter(objectMapper))
            }
        }

        pdlTjenesterKlient = PdlTjenesterKlient(httpClient, "/person")
    }

    @Test
    fun `skal hente person og mappe riktig til datamodell`() {
        mockEndpoint("/pdltjenester/person.json")

        val person = runBlocking {
            pdlTjenesterKlient.hentPerson(
                HentPersonRequest(
                    folkeregisteridentifikator = Folkeregisteridentifikator.of(FNR_1),
                    rolle = PersonRolle.BARN
                )
            )
        }

        assertNotNull(person)
        assertEquals(person.fornavn, "Jens")
        assertEquals(person.etternavn, "Johnsen")
    }
}