package person

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.routing.Route
import io.ktor.routing.routing
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import no.nav.etterlatte.libs.common.pdl.EyHentUtvidetPersonRequest
import no.nav.etterlatte.libs.common.person.EyFamilieRelasjon
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.Rolle
import no.nav.etterlatte.libs.common.person.eyAdresse
import no.nav.etterlatte.libs.common.person.eyUtland
import no.nav.etterlatte.person.PersonService
import no.nav.etterlatte.person.personApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test


class PersonRouteTest {

    private val personService = mockk<PersonService>().apply {
        coEvery { hentPerson(any()) } returns mockk(relaxed = true)
    }

    companion object {
        const val PERSON_ENDEPUNKT = "/person"
        const val GYLDIG_FNR = "07081177656"
    }

    @Test
    fun `skal feile med bad request dersom fnr ikke er oppgitt`() {
        withTestApplication({ testModule { personApi(personService) } }) {
            handleRequest(HttpMethod.Get, PERSON_ENDEPUNKT) {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }.apply {
                assertEquals(HttpStatusCode.BadRequest, response.status())
            }
        }
    }

    @Test
    @Disabled("Bør denne valideres i route? - skjer ikke før i service pr nå")
    fun `skal feile med bad request dersom fnr er ugyldig`() {
        withTestApplication({ testModule { personApi(personService) } }) {
            handleRequest(HttpMethod.Get, PERSON_ENDEPUNKT) {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                addHeader("foedselsnummer", "ugyldig_fnr")
            }.apply {
                assertEquals(HttpStatusCode.BadRequest, response.status())
            }
        }
    }

    @Test
    fun `skal returnere person uten noe utvidet informasjon`() {
        coEvery { personService.hentPerson(EyHentUtvidetPersonRequest(
            foedselsnummer = GYLDIG_FNR,
        )) } returns mockPerson()

        withTestApplication({ testModule { personApi(personService) } }) {
            handleRequest(HttpMethod.Get, PERSON_ENDEPUNKT) {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                addHeader("foedselsnummer", GYLDIG_FNR)
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                coVerify { personService.hentPerson(any()) }
                confirmVerified(personService)
            }
        }
    }

    @Test
    fun `skal returnere person med utvidet informasjon om utland`() {
        coEvery { personService.hentPerson(EyHentUtvidetPersonRequest(
            foedselsnummer = GYLDIG_FNR,
            historikk = true,
            utland = true
        )) } returns mockPerson()

        withTestApplication({ testModule { personApi(personService) } }) {
            handleRequest(HttpMethod.Get, "${PERSON_ENDEPUNKT}?historikk=true&utland=true") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                addHeader("foedselsnummer", GYLDIG_FNR)
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                coVerify { personService.hentPerson(any()) }
                confirmVerified(personService)
            }
        }
    }

    fun mockPerson(
        adresse: eyAdresse? = null,
        utland: eyUtland? = null,
        familieRelasjon: EyFamilieRelasjon? = null) =

        Person(
            fornavn = "Ola",
            etternavn = "Nordmann",
            foedselsnummer = Foedselsnummer.of(GYLDIG_FNR),
            foedselsaar = 2000,
            foedselsdato = "",
            doedsdato = null,
            adressebeskyttelse = false,
            adresse = adresse,
            statsborgerskap = "Norsk",
            foedeland = "Norge",
            sivilstatus = null,
            utland = utland,
            familieRelasjon = familieRelasjon,
            rolle = Rolle.BARN
        )

}

fun Application.testModule(routes: Route.() -> Unit) {
    install(ContentNegotiation) {
        jackson {
            enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)
            disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            registerModule(JavaTimeModule())
        }
    }

    routing {
        routes()
    }
}