package no.nav.etterlatte.pdl

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import io.ktor.serialization.jackson.jackson
import no.nav.etterlatte.libs.common.behandling.KorrektIPDL
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.person.UtflyttingFraNorge
import no.nav.etterlatte.libs.common.person.Utland
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.mockPerson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class PdlServiceTest {

    private val defaultHeaders = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))

    @Test
    fun `hent pdlModell skal returnere en Person`() {
        val klient = mockHttpClient(mockPerson())
        val pdlService = PdlService(klient, "url")
        val fnr = "70078749472"
        val rolle = PersonRolle.BARN
        val resultat = pdlService.hentPdlModell(fnr, rolle)
        assertEquals(resultat.fornavn, "Test")
        assertEquals(resultat.etternavn, "Testulfsen")
    }

    @Test
    fun `skal returnere KorrektIPDL JA dersom person har doedsdato i pdl`() {
        val klient = mockHttpClient(mockPerson(doedsdato = LocalDate.of(2022, 10, 8)))
        val pdlService = PdlService(klient, "url")
        val fnr = "70078749472"
        val resultat = pdlService.personErDoed(fnr)
        assertEquals(KorrektIPDL.JA, resultat)
    }

    @Test
    fun `skal returnere KorrektIPDL NEI dersom person ikke har doedsdato i pdl`() {
        val klient = mockHttpClient(mockPerson(doedsdato = null))
        val pdlService = PdlService(klient, "url")
        val fnr = "70078749472"
        val resultat = pdlService.personErDoed(fnr)
        assertEquals(KorrektIPDL.NEI, resultat)
    }

    @Test
    fun `skal returnere KorrektIPDL JA dersom person har utflytting fra Norge`() {
        val klient = mockHttpClient(
            mockPerson(
                utland = Utland(
                    innflyttingTilNorge = listOf(),
                    utflyttingFraNorge = listOf(
                        UtflyttingFraNorge(tilflyttingsland = "Sveits", dato = LocalDate.of(2022, 4, 3))
                    )
                )
            )
        )
        val pdlService = PdlService(klient, "url")
        val fnr = "70078749472"
        val resultat = pdlService.personHarUtflytting(fnr)
        assertEquals(KorrektIPDL.JA, resultat)
    }

    @Test
    fun `skal returnere KorrektIPDL NEI dersom person ikke har utflytting fra Norge`() {
        val klient = mockHttpClient(mockPerson())
        val pdlService = PdlService(klient, "url")
        val fnr = "70078749472"
        val resultat = pdlService.personHarUtflytting(fnr)
        assertEquals(KorrektIPDL.NEI, resultat)
    }

    private fun mockHttpClient(personRespons: Person): HttpClient {
        val httpClient = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    when (request.url.fullPath) {
                        "/url/person" -> respond(personRespons.toJson(), HttpStatusCode.OK, defaultHeaders)
                        else -> error("Unhandled ${request.url.fullPath}")
                    }
                }
            }
            expectSuccess = true
            install(ContentNegotiation) {
                jackson {
                    registerModule(JavaTimeModule())
                }
            }
        }

        return httpClient
    }
}