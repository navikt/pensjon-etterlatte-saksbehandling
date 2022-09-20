package no.nav.etterlatte.person

import GrunnlagTestData
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.* // ktlint-disable no-wildcard-imports
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import no.nav.etterlatte.SecurityContextMediatorStub
import no.nav.etterlatte.TRIVIELL_MIDTPUNKT
import no.nav.etterlatte.libs.common.person.HentFolkeregisterIdentRequest
import no.nav.etterlatte.libs.common.person.HentPersonRequest
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.mockFolkeregisterident
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

        coEvery { personService.hentPerson(hentPersonRequest) } returns GrunnlagTestData().søker

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
    fun `skal returnere personopplysninger paa version 2`() {
        val hentPersonRequest = HentPersonRequest(
            foedselsnummer = TRIVIELL_MIDTPUNKT,
            rolle = PersonRolle.BARN
        )

        coEvery { personService.hentOpplysningsperson(hentPersonRequest) } returns mockPerson()

        withTestApplication({ module(securityContextMediator, personService) }) {
            handleRequest(HttpMethod.Post, PERSON_ENDEPUNKT_V2) {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(hentPersonRequest.toJson())
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                coVerify { personService.hentOpplysningsperson(any()) }
                verify { securityContextMediator.secureRoute(any(), any()) }
                confirmVerified(personService)
            }
        }
    }

    @Test
    fun `skal returnere folkeregisterIdent`() {
        val hentFolkeregisterIdentRequest = HentFolkeregisterIdentRequest(
            ident = "2305469522806"
        )
        coEvery { personService.hentFolkeregisterIdent(hentFolkeregisterIdentRequest) } returns mockFolkeregisterident(
            "70078749472"
        )

        withTestApplication({ module(securityContextMediator, personService) }) {
            handleRequest(HttpMethod.Post, FOLKEREGISTERIDENT_ENDEPUNKT) {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(hentFolkeregisterIdentRequest.toJson())
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                coVerify { personService.hentFolkeregisterIdent(any()) }
                verify { securityContextMediator.secureRoute(any(), any()) }
                confirmVerified(personService)
            }
        }
    }

    @Test
    fun `skal returne 500 naar kall mot person feiler`() {
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

    @Test
    fun `skal returne 500 naar kall mot folkeregisterident feiler`() {
        val hentFolkeregisterIdentReq = HentFolkeregisterIdentRequest(
            ident = "2305469522806"
        )

        coEvery { personService.hentFolkeregisterIdent(hentFolkeregisterIdentReq) } throws PdlForesporselFeilet(
            "Noe feilet"
        )

        testApplication {
            application {
                module(securityContextMediator, personService)
            }
            client.post(FOLKEREGISTERIDENT_ENDEPUNKT) {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(hentFolkeregisterIdentReq.toJson())
            }.apply {
                assertEquals(HttpStatusCode.InternalServerError, status)
                assertEquals("En feil oppstod: Noe feilet", bodyAsText())
                coVerify { personService.hentFolkeregisterIdent(any()) }
                verify { securityContextMediator.secureRoute(any(), any()) }
                confirmVerified(personService)
            }
        }
    }

    companion object {
        const val PERSON_ENDEPUNKT = "/person"
        const val PERSON_ENDEPUNKT_V2 = "/person/v2"
        const val FOLKEREGISTERIDENT_ENDEPUNKT = "/folkeregisterident"
    }
}