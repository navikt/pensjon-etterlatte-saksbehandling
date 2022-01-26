package person

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.http.ContentType
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import io.mockk.coEvery
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.person.Pdl
import no.nav.etterlatte.person.PersonKlient
import no.nav.etterlatte.person.pdl.PersonResponse
import org.junit.jupiter.api.BeforeAll
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
    fun `hentPerson returnerer gyldig PersonResponse objekt`() {
        setup("/pdl/personResponse.json")
        runBlocking {
            val testPerson = personKlient.hentPerson(Foedselsnummer.of(STOR_SNERK))
            assertEquals("TRIVIELL", testPerson.data?.hentPerson?.navn?.get(0)?.fornavn)
            //TODO her kan vi evt teste flere felter
        }

    }
    @Test
    fun `hentUtland returnerer gyldig UtlandResponse objekt`() {
        setup("/pdl/utlandResponseFraflytting.json")
        runBlocking {
            val testPerson = personKlient.hentUtland(Foedselsnummer.of(STOR_SNERK))
            assertEquals("2021-07-01", testPerson.data?.hentPerson?.utflyttingFraNorge?.get(0)?.utflyttingsdato)
            //TODO her kan vi evt teste flere felter
        }

    }

}