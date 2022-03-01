package no.nav.etterlatte.person

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
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import no.nav.etterlatte.libs.common.person.Adresse
import no.nav.etterlatte.libs.common.person.Adressebeskyttelse
import no.nav.etterlatte.libs.common.person.FamilieRelasjon
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.HentPersonRequest
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.Utland
import no.nav.etterlatte.libs.common.toJson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate


class PersonRouteTest {

    private val personService = mockk<PersonService>().apply {
        coEvery { hentPerson(any()) } returns mockk(relaxed = true)
    }

    companion object {
        const val PERSON_ENDEPUNKT = "/person"
        const val GYLDIG_FNR = "07081177656"
    }

    @Test
    fun `skal returnere person`() {
        val hentPersonRequest = HentPersonRequest(
            foedselsnummer = Foedselsnummer.of(GYLDIG_FNR),
        )

        coEvery { personService.hentPerson(hentPersonRequest) } returns mockPerson()

        withTestApplication({ testModule { personApi(personService) } }) {
            handleRequest(HttpMethod.Post, PERSON_ENDEPUNKT) {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(hentPersonRequest.toJson())
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                coVerify { personService.hentPerson(any()) }
                confirmVerified(personService)
            }
        }
    }

    @Test
    fun `skal returne 500 naar kall mot service feiler`() {
        val hentPersonRequest = HentPersonRequest(
            foedselsnummer = Foedselsnummer.of(GYLDIG_FNR),
        )

        coEvery { personService.hentPerson(hentPersonRequest) } throws PdlForesporselFeilet("Noe feilet")

        withTestApplication({ testModule { personApi(personService) } }) {
            handleRequest(HttpMethod.Post, PERSON_ENDEPUNKT) {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(hentPersonRequest.toJson())
            }.apply {
                assertEquals(HttpStatusCode.InternalServerError, response.status())
                coVerify { personService.hentPerson(any()) }
                confirmVerified(personService)
            }
        }
    }

    fun mockPerson(
        adresse: Adresse? = null,
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
            adresse = adresse,
            statsborgerskap = "Norsk",
            foedeland = "Norge",
            sivilstatus = null,
            utland = utland,
            familieRelasjon = familieRelasjon
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