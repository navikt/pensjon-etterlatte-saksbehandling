package no.nav.etterlatte.person

import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import no.nav.etterlatte.TRIVIELL_MIDTPUNKT
import no.nav.etterlatte.ktor.runServer
import no.nav.etterlatte.ktor.token.issueSaksbehandlerToken
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.HentGeografiskTilknytningRequest
import no.nav.etterlatte.libs.common.person.HentPdlIdentRequest
import no.nav.etterlatte.libs.common.person.HentPersonRequest
import no.nav.etterlatte.libs.common.person.PersonIdent
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import no.nav.etterlatte.mockFolkeregisterident
import no.nav.etterlatte.mockGeografiskTilknytning
import no.nav.etterlatte.mockPerson
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PersonRouteTest {
    private val server = MockOAuth2Server()
    private val personService: PersonService = mockk()

    @BeforeAll
    fun before() {
        server.start()
    }

    @AfterAll
    fun after() {
        server.shutdown()
    }

    @Test
    fun `skal returnere person`() {
        val hentPersonRequest =
            HentPersonRequest(
                foedselsnummer = TRIVIELL_MIDTPUNKT,
                rolle = PersonRolle.BARN,
                saktyper = listOf(SakType.BARNEPENSJON),
            )

        coEvery { personService.hentPerson(hentPersonRequest) } returns GrunnlagTestData().soeker

        testApplication {
            runServer(server) {
                personRoute(personService)
            }

            val response =
                client.post(PERSON_ENDEPUNKT) {
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
        val hentPersonRequest =
            HentPersonRequest(
                foedselsnummer = TRIVIELL_MIDTPUNKT,
                rolle = PersonRolle.BARN,
                saktyper = listOf(SakType.BARNEPENSJON),
            )

        coEvery { personService.hentOpplysningsperson(hentPersonRequest) } returns mockPerson()

        testApplication {
            runServer(server) {
                personRoute(personService)
            }

            val response =
                client.post(PERSON_ENDEPUNKT_V2) {
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
        val hentPdlIdentRequest = HentPdlIdentRequest(ident = PersonIdent(AVDOED_FOEDSELSNUMMER.value))
        coEvery {
            personService.hentPdlIdentifikator(hentPdlIdentRequest)
        } returns
            mockFolkeregisterident(
                "09508229892",
            )

        testApplication {
            runServer(server) {
                personRoute(personService)
            }

            val response =
                client.post(PDLIDENT_ENDEPUNKT) {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                    setBody(hentPdlIdentRequest.toJson())
                }

            assertEquals(HttpStatusCode.OK, response.status)
            coVerify { personService.hentPdlIdentifikator(any()) }
            confirmVerified(personService)
        }
    }

    @Test
    fun `skal returnere geografisk tilknytning`() {
        val hentGeografiskTilknytningRequest =
            HentGeografiskTilknytningRequest(
                foedselsnummer = TRIVIELL_MIDTPUNKT,
                saktype = SakType.BARNEPENSJON,
            )

        coEvery {
            personService.hentGeografiskTilknytning(hentGeografiskTilknytningRequest)
        } returns mockGeografiskTilknytning()

        testApplication {
            runServer(server) {
                personRoute(personService)
            }

            val response =
                client.post(GEOGRAFISKTILKNYTNING_ENDEPUNKT) {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                    setBody(hentGeografiskTilknytningRequest.toJson())
                }

            assertEquals(HttpStatusCode.OK, response.status)
            coVerify { personService.hentGeografiskTilknytning(any()) }
            confirmVerified(personService)
        }
    }

    @Test
    fun `skal returne 500 naar kall mot person feiler`() {
        val hentPersonRequest =
            HentPersonRequest(
                foedselsnummer = TRIVIELL_MIDTPUNKT,
                rolle = PersonRolle.BARN,
                saktyper = listOf(SakType.BARNEPENSJON),
            )

        coEvery {
            personService.hentPerson(hentPersonRequest)
        } throws PdlForesporselFeilet("Noe feilet")

        testApplication {
            runServer(server) {
                personRoute(personService)
            }

            val response =
                client.post(PERSON_ENDEPUNKT) {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                    setBody(hentPersonRequest.toJson())
                }

            assertEquals(HttpStatusCode.InternalServerError, response.status)
            coVerify { personService.hentPerson(any()) }
            confirmVerified(personService)
        }
    }

    @Test
    fun `skal returne 500 naar kall mot folkeregisterident feiler`() {
        val hentFolkeregisterIdentReq = HentPdlIdentRequest(ident = PersonIdent(AVDOED_FOEDSELSNUMMER.value))

        coEvery {
            personService.hentPdlIdentifikator(hentFolkeregisterIdentReq)
        } throws
            PdlForesporselFeilet(
                "Noe feilet",
            )

        testApplication {
            runServer(server) {
                personRoute(personService)
            }

            val response =
                client.post(PERSON_ENDEPUNKT) {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                    setBody(hentFolkeregisterIdentReq.toJson())
                }

            assertEquals(HttpStatusCode.InternalServerError, response.status)
            coVerify { personService.hentPdlIdentifikator(any()) }
            confirmVerified(personService)
        }
    }

    private val token: String by lazy { server.issueSaksbehandlerToken() }

    private companion object {
        const val PERSON_ENDEPUNKT = "/person"
        const val PERSON_ENDEPUNKT_V2 = "/person/v2"
        const val PDLIDENT_ENDEPUNKT = "/pdlident"
        const val GEOGRAFISKTILKNYTNING_ENDEPUNKT = "/geografisktilknytning"
    }
}
