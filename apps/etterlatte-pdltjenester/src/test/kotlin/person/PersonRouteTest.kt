package no.nav.etterlatte.person

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import no.nav.etterlatte.SecurityContextMediatorStub
import no.nav.etterlatte.TRIVIELL_MIDTPUNKT
import no.nav.etterlatte.libs.common.person.HentPersonRequest
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.mockPerson
import no.nav.etterlatte.module
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test


class PersonRouteTest {

    private val personService = mockk<PersonService>()
    private val securityContextMediator = spyk<SecurityContextMediatorStub>()

    @Test
    fun `skal returnere person`() {
        val hentPersonRequest = HentPersonRequest(
            foedselsnummer = TRIVIELL_MIDTPUNKT,
            rolle = PersonRolle.BARN
        )

        coEvery { personService.hentPerson(hentPersonRequest) } returns mockPerson()

        withTestApplication({ module(securityContextMediator, personService) }) {
            handleRequest(HttpMethod.Post, PERSON_ENDEPUNKT) {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(hentPersonRequest.toJson())
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                coVerify { personService.hentPerson(any()) }
                verify { securityContextMediator.secureRoute(any(), any()) }
                confirmVerified(personService)
            }
        }
    }

    @Test
    fun `skal returne 500 naar kall mot service feiler`() {
        val hentPersonRequest = HentPersonRequest(
            foedselsnummer = TRIVIELL_MIDTPUNKT,
            rolle = PersonRolle.BARN
        )

        coEvery { personService.hentPerson(hentPersonRequest) } throws PdlForesporselFeilet("Noe feilet")

        withTestApplication({ module(securityContextMediator, personService) }) {
            handleRequest(HttpMethod.Post, PERSON_ENDEPUNKT) {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(hentPersonRequest.toJson())
            }.apply {
                assertEquals(HttpStatusCode.InternalServerError, response.status())
                assertEquals("En feil oppstod: Noe feilet", response.content)
                coVerify { personService.hentPerson(any()) }
                verify { securityContextMediator.secureRoute(any(), any()) }
                confirmVerified(personService)
            }
        }
    }

    companion object {
        const val PERSON_ENDEPUNKT = "/person"
    }
}