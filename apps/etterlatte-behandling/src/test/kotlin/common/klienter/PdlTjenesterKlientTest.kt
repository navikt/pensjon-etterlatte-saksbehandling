package no.nav.etterlatte.common.klienter

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import io.ktor.serialization.jackson.jackson
import io.mockk.mockk
import no.nav.etterlatte.JOVIAL_LAMA
import no.nav.etterlatte.KONTANT_FOT
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.FamilieRelasjon
import no.nav.etterlatte.libs.common.person.GeografiskTilknytning
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.person.UtflyttingFraNorge
import no.nav.etterlatte.libs.common.person.Utland
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.mockPerson
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class PdlTjenesterKlientTest {
    private val defaultHeaders = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))

    private val config: Config = mockk(relaxed = true)

    @Test
    fun `hent pdlModell skal returnere en Person`() {
        val klient = mockHttpClient(mockPerson())
        val pdlService = PdlTjenesterKlientImpl(config, klient)
        val fnr = KONTANT_FOT
        val rolle = PersonRolle.BARN
        val resultat = pdlService.hentPdlModell(fnr.value, rolle, SakType.BARNEPENSJON)
        Assertions.assertEquals("Ola", resultat.fornavn.verdi)
        Assertions.assertEquals("Mellom", resultat.mellomnavn?.verdi)
        Assertions.assertEquals("Nordmann", resultat.etternavn.verdi)
    }

    @Test
    fun `skal hente doedsdato`() {
        val klient = mockHttpClient(mockPerson())
        val pdlService = PdlTjenesterKlientImpl(config, klient)
        val fnr = KONTANT_FOT
        val rolle = PersonRolle.BARN
        val resultat = pdlService.hentPdlModell(fnr.value, rolle, SakType.BARNEPENSJON).hentDoedsdato()
        Assertions.assertEquals(mockPerson().doedsdato?.verdi, resultat)
    }

    @Test
    fun `skal hente ansvarlige foreldre`() {
        val familierelasjon = FamilieRelasjon(ansvarligeForeldre = listOf(JOVIAL_LAMA), barn = null, foreldre = null)
        val mockperson = mockPerson(familieRelasjon = familierelasjon)
        val klient = mockHttpClient(mockperson)
        val pdlService = PdlTjenesterKlientImpl(config, klient)
        val fnr = KONTANT_FOT
        val rolle = PersonRolle.BARN
        val resultat = pdlService.hentPdlModell(fnr.value, rolle, SakType.BARNEPENSJON).hentAnsvarligeForeldre()
        Assertions.assertEquals(familierelasjon.ansvarligeForeldre, resultat)
    }

    @Test
    fun `skal hente barn`() {
        val familierelasjon = FamilieRelasjon(barn = listOf(JOVIAL_LAMA), ansvarligeForeldre = null, foreldre = null)
        val mockperson = mockPerson(familieRelasjon = familierelasjon)
        val klient = mockHttpClient(mockperson)
        val pdlService = PdlTjenesterKlientImpl(config, klient)
        val fnr = KONTANT_FOT
        val rolle = PersonRolle.BARN
        val resultat = pdlService.hentPdlModell(fnr.value, rolle, SakType.BARNEPENSJON).hentBarn()
        Assertions.assertEquals(familierelasjon.barn, resultat)
    }

    @Test
    fun `skal hente geografisk tilknytning`() {
        val klient = mockHttpClient(GeografiskTilknytning(kommune = "0301", ukjent = false))
        val pdlService = PdlTjenesterKlientImpl(config, klient)
        val fnr = KONTANT_FOT
        val resultat = pdlService.hentGeografiskTilknytning(fnr.value, SakType.BARNEPENSJON).geografiskTilknytning()
        Assertions.assertEquals("0301", resultat)
    }

    @Test
    fun `skal hente utland`() {
        val utland =
            Utland(
                innflyttingTilNorge = null,
                utflyttingFraNorge =
                    listOf(
                        UtflyttingFraNorge(
                            tilflyttingsland = "Sverige",
                            dato = LocalDate.now(),
                        ),
                    ),
            )
        val mockperson = mockPerson(utland = utland)
        val klient = mockHttpClient(mockperson)
        val pdlService = PdlTjenesterKlientImpl(config, klient)
        val fnr = KONTANT_FOT
        val rolle = PersonRolle.BARN
        val resultat = pdlService.hentPdlModell(fnr.value, rolle, SakType.BARNEPENSJON).hentUtland()
        Assertions.assertEquals(utland, resultat)
    }

    private fun mockHttpClient(respons: Any): HttpClient {
        val httpClient =
            HttpClient(MockEngine) {
                engine {
                    addHandler { request ->
                        when (request.url.fullPath) {
                            "/geografisktilknytning" -> respond(respons.toJson(), HttpStatusCode.OK, defaultHeaders)
                            "/person/v2" -> respond(respons.toJson(), HttpStatusCode.OK, defaultHeaders)
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
