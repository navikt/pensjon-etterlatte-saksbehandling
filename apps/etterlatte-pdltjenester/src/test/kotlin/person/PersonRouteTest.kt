package no.nav.etterlatte.person

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.routing.Route
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.invoke
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.etterlatte.ktortokenexchange.SecurityContextMediator
import no.nav.etterlatte.libs.common.person.Adresse
import no.nav.etterlatte.libs.common.person.AdresseType
import no.nav.etterlatte.libs.common.person.Adressebeskyttelse
import no.nav.etterlatte.libs.common.person.FamilieRelasjon
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.HentPersonRequest
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.person.Utland
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.module
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime


class PersonRouteTest {

    private val personService = mockk<PersonService>()
    private val mockedSecurityContextMediator = mockk<SecurityContextMediator>(relaxed = true).apply {
        val slot = slot<Route>()
        every { secureRoute(capture(slot), captureLambda()) } answers { lambda<(Route) -> Unit>().invoke(slot.captured) }
    }

    @Test
    fun `skal returnere person`() {
        val hentPersonRequest = HentPersonRequest(
            foedselsnummer = Foedselsnummer.of(GYLDIG_FNR),
            rolle = PersonRolle.BARN
        )

        coEvery { personService.hentPerson(hentPersonRequest) } returns mockPerson()

        withTestApplication({ module(mockedSecurityContextMediator, personService) }) {
            handleRequest(HttpMethod.Post, PERSON_ENDEPUNKT) {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(hentPersonRequest.toJson())
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                coVerify { personService.hentPerson(any()) }
                verify { mockedSecurityContextMediator.secureRoute(any(), any()) }
                confirmVerified(personService)
            }
        }
    }

    @Test
    fun `skal returne 500 naar kall mot service feiler`() {
        val hentPersonRequest = HentPersonRequest(
            foedselsnummer = Foedselsnummer.of(GYLDIG_FNR),
            rolle = PersonRolle.BARN
        )

        coEvery { personService.hentPerson(hentPersonRequest) } throws PdlForesporselFeilet("Noe feilet")

        withTestApplication({ module(mockedSecurityContextMediator, personService) }) {
            handleRequest(HttpMethod.Post, PERSON_ENDEPUNKT) {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(hentPersonRequest.toJson())
            }.apply {
                assertEquals(HttpStatusCode.InternalServerError, response.status())
                assertEquals("En feil oppstod: Noe feilet", response.content)
                coVerify { personService.hentPerson(any()) }
                verify { mockedSecurityContextMediator.secureRoute(any(), any()) }
                confirmVerified(personService)
            }
        }
    }

    fun mockPerson(
        utland: Utland? = null,
        familieRelasjon: FamilieRelasjon? = null) =

        Person(
            fornavn = "Ola",
            etternavn = "Nordmann",
            foedselsnummer = Foedselsnummer.of(GYLDIG_FNR),
            foedselsaar = 2000,
            foedselsdato = LocalDate.now().minusYears(20),
            doedsdato = null,
            adressebeskyttelse = Adressebeskyttelse.UGRADERT,
            bostedsadresse = listOf(
                Adresse(
                    type = AdresseType.VEGADRESSE,
                    aktiv = true,
                    coAdresseNavn = "Hos Geir",
                    adresseLinje1 = "Testveien 4",
                    adresseLinje2 = null,
                    adresseLinje3 = null,
                    postnr = "1234",
                    poststed = null,
                    land = "NOR",
                    kilde = "FREG",
                    gyldigFraOgMed = LocalDateTime.now().minusYears(1),
                    gyldigTilOgMed = null
                )
            ),
            deltBostedsadresse = emptyList(),
            oppholdsadresse = emptyList(),
            kontaktadresse = emptyList(),
            statsborgerskap = "Norsk",
            foedeland = "Norge",
            sivilstatus = null,
            utland = utland,
            familieRelasjon = familieRelasjon
        )

    companion object {
        const val PERSON_ENDEPUNKT = "/person"
        const val GYLDIG_FNR = "07081177656"
    }
}