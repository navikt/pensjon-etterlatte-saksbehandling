package no.nav.etterlatte.pdl

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.http.ContentType
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.STOR_SNERK
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.PersonRolle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class PdlKlientTest {

    private lateinit var pdlKlient: PdlKlient

    @Test
    fun `hentPerson returnerer gyldig person`() {
        mockEndpoint("/pdl/person.json")

        runBlocking {
            val personResponse = pdlKlient.hentPerson(STOR_SNERK, PersonRolle.BARN)
            val hentPerson = personResponse.data?.hentPerson

            assertEquals("LITEN", hentPerson?.navn?.first()?.fornavn)
            assertEquals("HEST", hentPerson?.navn?.first()?.etternavn)
            assertEquals("2007-08-29", hentPerson?.foedsel?.first()?.foedselsdato?.toString())
            assertEquals("NIC", hentPerson?.foedsel?.first()?.foedeland)
            // TODO sjekk flere relevante felter
        }
    }

    @Test
    fun `hentPersonBolk returnerer gyldige personer`() {
        mockEndpoint("/pdl/personBolk.json")

        runBlocking {
            val personResponse = pdlKlient.hentPersonBolk(listOf(STOR_SNERK), PersonRolle.BARN)
            val hentPerson = personResponse.data?.hentPersonBolk

            assertEquals("TRIVIELL", hentPerson?.first()?.person?.navn?.first()?.fornavn)
            assertEquals("SKILPADDE", hentPerson?.first()?.person?.navn?.first()?.etternavn)
            assertEquals("1987-07-30", hentPerson?.first()?.person?.foedsel?.first()?.foedselsdato?.toString())
            assertEquals("FJI", hentPerson?.first()?.person?.foedsel?.first()?.foedeland)
            // TODO sjekk flere relevante felter
        }
    }

    @Test
    fun `hentPerson returnerer ikke funnet`() {
        mockEndpoint("/pdl/person_ikke_funnet.json")

        runBlocking {
            val personResponse = pdlKlient.hentPerson(STOR_SNERK, PersonRolle.BARN)
            val errors = personResponse.errors

            assertEquals("Fant ikke person", errors?.first()?.message)
        }
    }

    @Test
    fun `hentFolkeregisterIdent returnerer folkeresiterident`() {
        mockEndpoint("/pdl/folkeregisterident.json")

        runBlocking {
            val identResponse = pdlKlient.hentFolkeregisterIdent("2305469522806")
            assertEquals("70078749472", identResponse.data?.hentIdenter?.identer?.first()?.ident)
            assertEquals(false, identResponse.data?.hentIdenter?.identer?.first()?.historisk)
            assertEquals("FOLKEREGISTERIDENT", identResponse.data?.hentIdenter?.identer?.first()?.gruppe)
        }
    }

    @Test
    fun `hentFolkeregisterIdent returnerer ikke funnet`() {
        mockEndpoint("/pdl/ident_ikke_funnet.json")

        runBlocking {
            val personResponse = pdlKlient.hentFolkeregisterIdent("1234")
            val errors = personResponse.errors

            assertEquals("Fant ikke person", errors?.first()?.message)
        }
    }

    private fun mockEndpoint(jsonUrl: String) {
        val httpClient = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    when (request.url.fullPath) {
                        "/" -> {
                            val headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                            val json = javaClass.getResource(jsonUrl)!!.readText()
                            respond(json, headers = headers)
                        }
                        else -> error(request.url.fullPath)
                    }
                }
            }
            install(JsonFeature) { serializer = JacksonSerializer(objectMapper) }
        }

        pdlKlient = PdlKlient(httpClient)
    }

}