package person

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.http.ContentType
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.pdl.Variables

import no.nav.etterlatte.person.Pdl
import no.nav.etterlatte.person.PersonKlient

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PersonKlientTest {

    private companion object {
        private const val TREIG_FLOSKEL = "04096222195"
        private const val TRIVIELL_MIDTPUNKT = "19040550081"
        private const val STOR_SNERK = "11057523044"
    }
    private lateinit var personKlient: Pdl


    fun setup(jsonUrl: String) {
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
            install(JsonFeature) { serializer = JacksonSerializer() }
        }

        personKlient = PersonKlient(httpClient)
    }

    @Test
    fun `hentUtvidetPerson returnerer gyldig UtvidetPersonResponse objekt`() {
        setup("/pdl/personUtvidetResponse.json")
        runBlocking {
            val testPerson = personKlient.hentPerson(Variables(STOR_SNERK))
            assertEquals("LITEN", testPerson.data?.hentPerson?.navn?.get(0)?.fornavn)
            //TODO her kan vi evt teste flere felter
        }
    }

    @Test
    fun `hentPerson returnerer gyldig PersonResponse objekt`() {
        setup("/pdl/personResponse.json")
        runBlocking {
            val testPerson = personKlient.hentPerson(Variables(STOR_SNERK))
            assertEquals("TRIVIELL", testPerson.data?.hentPerson?.navn?.get(0)?.fornavn)
            //TODO her kan vi evt teste flere felter
        }

    }


}