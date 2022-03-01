package no.nav.etterlatte.prosess.pdltjenester

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.HentPersonRequest
import no.nav.etterlatte.pdltjenester.PdlTjenesterKlient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class PdlTjenesterKlientTest {

    private lateinit var pdlTjenesterKlient: PdlTjenesterKlient

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
            install(JsonFeature) { serializer = JacksonSerializer(objectMapper) }
        }

        pdlTjenesterKlient = PdlTjenesterKlient(httpClient, "/person")
    }

    @Test
    fun `skal hente person og mappe riktig til datamodell`() {
        mockEndpoint("/pdltjenester/person.json")

        val person = runBlocking {
            pdlTjenesterKlient.hentPerson(HentPersonRequest(
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