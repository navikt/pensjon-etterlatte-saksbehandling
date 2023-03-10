package no.nav.etterlatte.person

import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.log
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import no.nav.etterlatte.TRIVIELL_MIDTPUNKT
import no.nav.etterlatte.libs.common.person.HentFolkeregisterIdentRequest
import no.nav.etterlatte.libs.common.person.HentPersonRequest
import no.nav.etterlatte.libs.common.person.PersonIdent
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.ktor.AZURE_ISSUER
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import no.nav.etterlatte.mockFolkeregisterident
import no.nav.etterlatte.mockPerson
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import testsupport.buildTestApplicationConfigurationForOauth

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PersonRouteTest {

    private val server = MockOAuth2Server()
    private lateinit var applicationConfig: ApplicationConfig
    private val personService: PersonService = mockk()

    @BeforeAll
    fun before() {
        server.start()
        applicationConfig =
            buildTestApplicationConfigurationForOauth(server.config.httpServer.port(), AZURE_ISSUER, CLIENT_ID)
    }

    @AfterAll
    fun after() {
        server.shutdown()
    }

    @Test
    fun `skal returnere person`() {
        val hentPersonRequest = HentPersonRequest(
            foedselsnummer = TRIVIELL_MIDTPUNKT,
            rolle = PersonRolle.BARN
        )

        coEvery { personService.hentPerson(hentPersonRequest) } returns GrunnlagTestData().soeker

        testApplication {
            environment {
                config = applicationConfig
            }
            application {
                restModule(log) { personRoute(personService) }
            }

            val response = client.post(PERSON_ENDEPUNKT) {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(hentPersonRequest.toJson())
            }

            assertEquals(HttpStatusCode.OK, response.status)
            coVerify { personService.hentPerson(any()) }
            confirmVerified(personService)
        }
    }

    @Test
    fun `skal returnere personopplysninger paa version 2`() {
        val hentPersonRequest = HentPersonRequest(
            foedselsnummer = TRIVIELL_MIDTPUNKT,
            rolle = PersonRolle.BARN
        )

        coEvery { personService.hentOpplysningsperson(hentPersonRequest) } returns mockPerson()

        testApplication {
            environment {
                config = applicationConfig
            }
            application {
                restModule(log) { personRoute(personService) }
            }

            val response = client.post(PERSON_ENDEPUNKT_V2) {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(hentPersonRequest.toJson())
            }

            assertEquals(HttpStatusCode.OK, response.status)
            coVerify { personService.hentOpplysningsperson(any()) }
            confirmVerified(personService)
        }
    }

    @Test
    fun `skal returnere folkeregisterIdent`() {
        val hentFolkeregisterIdentRequest = HentFolkeregisterIdentRequest(
            ident = PersonIdent("2305469522806")
        )
        coEvery {
            personService.hentFolkeregisterIdent(hentFolkeregisterIdentRequest)
        } returns mockFolkeregisterident(
            "70078749472"
        )

        testApplication {
            environment {
                config = applicationConfig
            }
            application {
                restModule(log) { personRoute(personService) }
            }

            val response = client.post(FOLKEREGISTERIDENT_ENDEPUNKT) {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(hentFolkeregisterIdentRequest.toJson())
            }

            assertEquals(HttpStatusCode.OK, response.status)
            coVerify { personService.hentFolkeregisterIdent(any()) }
            confirmVerified(personService)
        }
    }

    @Test
    fun `skal returne 500 naar kall mot person feiler`() {
        val hentPersonRequest = HentPersonRequest(
            foedselsnummer = TRIVIELL_MIDTPUNKT,
            rolle = PersonRolle.BARN
        )

        coEvery {
            personService.hentPerson(hentPersonRequest)
        } throws PdlForesporselFeilet("Noe feilet")

        testApplication {
            environment {
                config = applicationConfig
            }
            application {
                restModule(log) { personRoute(personService) }
            }

            val response = client.post(PERSON_ENDEPUNKT) {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(hentPersonRequest.toJson())
            }

            assertEquals(HttpStatusCode.InternalServerError, response.status)
            assertEquals("En intern feil har oppstått", response.bodyAsText())
            coVerify { personService.hentPerson(any()) }
            confirmVerified(personService)
        }
    }

    @Test
    fun `skal returne 500 naar kall mot folkeregisterident feiler`() {
        val hentFolkeregisterIdentReq = HentFolkeregisterIdentRequest(
            ident = PersonIdent("2305469522806")
        )

        coEvery {
            personService.hentFolkeregisterIdent(hentFolkeregisterIdentReq)
        } throws PdlForesporselFeilet(
            "Noe feilet"
        )

        testApplication {
            environment {
                config = applicationConfig
            }
            application {
                restModule(log) { personRoute(personService) }
            }

            val response = client.post(PERSON_ENDEPUNKT) {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(hentFolkeregisterIdentReq.toJson())
            }

            assertEquals(HttpStatusCode.InternalServerError, response.status)
            assertEquals("En intern feil har oppstått", response.bodyAsText())
            coVerify { personService.hentFolkeregisterIdent(any()) }
            confirmVerified(personService)
        }
    }

    private val token: String by lazy {
        server.issueToken(
            issuerId = AZURE_ISSUER,
            audience = CLIENT_ID,
            claims = mapOf(
                "navn" to "John Doe",
                "NAVident" to "Saksbehandler01"
            )
        ).serialize()
    }

    private companion object {
        const val PERSON_ENDEPUNKT = "/person"
        const val PERSON_ENDEPUNKT_V2 = "/person/v2"
        const val FOLKEREGISTERIDENT_ENDEPUNKT = "/folkeregisterident"
        const val CLIENT_ID = "azure-id for saksbehandler"
    }
}