package no.nav.etterlatte.klienter

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
import no.nav.etterlatte.STOR_SNERK
import no.nav.etterlatte.TRIVIELL_MIDTPUNKT
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.person.FamilieRelasjon
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.person.UtflyttingFraNorge
import no.nav.etterlatte.libs.common.person.Utland
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.mockPerson
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class PdlKlientTest {

    private val defaultHeaders = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))

    @Test
    fun `hent pdlModell skal returnere en Person`() {
        val klient = mockHttpClient(mockPerson())
        val pdlService = PdlKlientImpl(klient, "url")
        val fnr = TRIVIELL_MIDTPUNKT
        val rolle = PersonRolle.BARN
        val resultat = pdlService.hentPdlModell(fnr.value, rolle)
        Assertions.assertEquals("Ola", resultat.fornavn.verdi)
        Assertions.assertEquals("Nordmann", resultat.etternavn.verdi)
    }

    @Test
    fun `skal hente doedsdato`() {
        val klient = mockHttpClient(mockPerson())
        val pdlService = PdlKlientImpl(klient, "url")
        val fnr = TRIVIELL_MIDTPUNKT
        val rolle = PersonRolle.BARN
        val resultat = pdlService.hentPdlModell(fnr.value, rolle).hentDoedsdato()
        Assertions.assertEquals(mockPerson().doedsdato?.verdi, resultat)
    }

    @Test
    fun `skal hente ansvarlige foreldre`() {
        val familierelasjon = FamilieRelasjon(ansvarligeForeldre = listOf(STOR_SNERK), barn = null, foreldre = null)
        val mockperson = mockPerson(familieRelasjon = familierelasjon)
        val klient = mockHttpClient(mockperson)
        val pdlService = PdlKlientImpl(klient, "url")
        val fnr = TRIVIELL_MIDTPUNKT
        val rolle = PersonRolle.BARN
        val resultat = pdlService.hentPdlModell(fnr.value, rolle).hentAnsvarligeForeldre()
        Assertions.assertEquals(familierelasjon.ansvarligeForeldre, resultat)
    }

    @Test
    fun `skal hente barn`() {
        val familierelasjon = FamilieRelasjon(barn = listOf(STOR_SNERK), ansvarligeForeldre = null, foreldre = null)
        val mockperson = mockPerson(familieRelasjon = familierelasjon)
        val klient = mockHttpClient(mockperson)
        val pdlService = PdlKlientImpl(klient, "url")
        val fnr = TRIVIELL_MIDTPUNKT
        val rolle = PersonRolle.BARN
        val resultat = pdlService.hentPdlModell(fnr.value, rolle).hentBarn()
        Assertions.assertEquals(familierelasjon.barn, resultat)
    }

    @Test
    fun `skal hente utland`() {
        val utland = Utland(
            innflyttingTilNorge = null,
            utflyttingFraNorge = listOf(
                UtflyttingFraNorge(
                    tilflyttingsland = "Sverige",
                    dato = LocalDate.now()
                )
            )
        )
        val mockperson = mockPerson(utland = utland)
        val klient = mockHttpClient(mockperson)
        val pdlService = PdlKlientImpl(klient, "url")
        val fnr = TRIVIELL_MIDTPUNKT
        val rolle = PersonRolle.BARN
        val resultat = pdlService.hentPdlModell(fnr.value, rolle).hentUtland()
        Assertions.assertEquals(utland, resultat)
    }

    private fun mockHttpClient(personRespons: PersonDTO): HttpClient {
        val httpClient = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    when (request.url.fullPath) {
                        "/url/person/v2" -> respond(personRespons.toJson(), HttpStatusCode.OK, defaultHeaders)
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