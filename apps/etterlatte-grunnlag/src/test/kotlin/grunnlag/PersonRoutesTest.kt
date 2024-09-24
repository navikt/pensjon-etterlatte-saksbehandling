package no.nav.etterlatte.grunnlag

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.Called
import io.mockk.clearAllMocks
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.grunnlag.klienter.BehandlingKlient
import no.nav.etterlatte.ktor.runServer
import no.nav.etterlatte.ktor.startRandomPort
import no.nav.etterlatte.ktor.token.issueSystembrukerToken
import no.nav.etterlatte.libs.common.behandling.PersonMedSakerOgRoller
import no.nav.etterlatte.libs.common.behandling.SakidOgRolle
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.serialize
import no.nav.etterlatte.libs.ktor.route.FoedselsnummerDTO
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PersonRoutesTest {
    private val grunnlagService = mockk<GrunnlagService>()
    private val behandlingKlient = mockk<BehandlingKlient>()
    private val mockOAuth2Server = MockOAuth2Server()

    @BeforeAll
    fun before() {
        mockOAuth2Server.startRandomPort()
    }

    @AfterEach
    fun afterEach() {
        confirmVerified(grunnlagService, behandlingKlient)
        clearAllMocks()
    }

    @AfterAll
    fun after() {
        mockOAuth2Server.shutdown()
    }

    @Test
    fun `returnerer 401 uten gyldig token`() {
        testApplication {
            runServer(mockOAuth2Server, "api/grunnlag") {
                personRoute(grunnlagService, behandlingKlient)
            }

            val response = client.post("api/grunnlag/person/saker")

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

        coVerify(exactly = 1) { behandlingKlient wasNot Called }
    }

    @Test
    fun `Hent alle roller tilknyttet person`() {
        val response =
            PersonMedSakerOgRoller(
                SOEKER_FOEDSELSNUMMER.value,
                listOf(
                    SakidOgRolle(1, Saksrolle.SOEKER),
                ),
            )
        every { grunnlagService.hentSakerOgRoller(any()) } returns response

        testApplication {
            val httpClient = createHttpClient()
            val actualResponse =
                httpClient.post("api/grunnlag/person/roller") {
                    setBody(FoedselsnummerDTO(SOEKER_FOEDSELSNUMMER.value))
                    headers {
                        append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        append(HttpHeaders.Authorization, "Bearer ${mockOAuth2Server.issueSystembrukerToken()}")
                    }
                }

            assertEquals(HttpStatusCode.OK, actualResponse.status)
            assertEquals(serialize(response), actualResponse.body<String>())
        }

        verify(exactly = 1) { grunnlagService.hentSakerOgRoller(any()) }
        coVerify { behandlingKlient wasNot Called }
    }

    @Test
    fun `Hent alle saker tilknyttet person`() {
        val response: Set<SakId> = setOf(1, 2, 3)
        every { grunnlagService.hentAlleSakerForFnr(any()) } returns response

        testApplication {
            val httpClient = createHttpClient()
            val actualResponse =
                httpClient.post("api/grunnlag/person/saker") {
                    contentType(ContentType.Application.Json)
                    setBody(FoedselsnummerDTO(SOEKER_FOEDSELSNUMMER.value))
                    headers {
                        append(HttpHeaders.Authorization, "Bearer ${mockOAuth2Server.issueSystembrukerToken()}")
                    }
                }

            assertEquals(HttpStatusCode.OK, actualResponse.status)
            assertEquals(serialize(response), actualResponse.body<String>())
        }

        verify(exactly = 1) { grunnlagService.hentAlleSakerForFnr(any()) }
        coVerify { behandlingKlient wasNot Called }
    }

    private fun ApplicationTestBuilder.createHttpClient(): HttpClient =
        runServer(mockOAuth2Server, "api/grunnlag") {
            personRoute(grunnlagService, behandlingKlient)
        }
}
