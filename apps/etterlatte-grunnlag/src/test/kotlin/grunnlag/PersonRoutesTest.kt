package no.nav.etterlatte.grunnlag

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.log
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.Called
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.libs.common.FoedselsnummerDTO
import no.nav.etterlatte.libs.common.behandling.PersonMedSakerOgRoller
import no.nav.etterlatte.libs.common.behandling.SakOgRolle
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.serialize
import no.nav.etterlatte.libs.ktor.AZURE_ISSUER
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import testsupport.buildTestApplicationConfigurationForOauth
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PersonRoutesTest {
    private val grunnlagService = mockk<GrunnlagService>()
    private val behandlingKlient = mockk<BehandlingKlient>()
    private val server = MockOAuth2Server()
    private lateinit var hoconApplicationConfig: HoconApplicationConfig

    companion object {
        private const val CLIENT_ID = "CLIENT_ID"
    }

    @BeforeAll
    fun before() {
        server.start()
        val httpServer = server.config.httpServer
        hoconApplicationConfig = buildTestApplicationConfigurationForOauth(httpServer.port(), AZURE_ISSUER, CLIENT_ID)
    }

    @AfterEach
    fun afterEach() {
        confirmVerified(grunnlagService, behandlingKlient)
        clearAllMocks()
    }

    @AfterAll
    fun after() {
        server.shutdown()
    }

    private val saksbehandlerToken by lazy {
        server.issueToken(
            issuerId = AZURE_ISSUER,
            audience = CLIENT_ID,
            claims =
                mapOf(
                    "navn" to "Per Persson",
                    "NAVident" to "Saksbehandler01",
                ),
        ).serialize()
    }

    private val systembrukerToken: String by lazy {
        val mittsystem = UUID.randomUUID().toString()
        server.issueToken(
            issuerId = AZURE_ISSUER,
            audience = CLIENT_ID,
            claims =
                mapOf(
                    "sub" to mittsystem,
                    "oid" to mittsystem,
                ),
        ).serialize()
    }

    @Test
    fun `returnerer 401 uten gyldig token`() {
        testApplication {
            environment {
                config = hoconApplicationConfig
            }
            application {
                restModule(this.log, routePrefix = "api/grunnlag") { personRoute(grunnlagService, behandlingKlient) }
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
                    SakOgRolle(1, Saksrolle.SOEKER),
                ),
            )
        every { grunnlagService.hentSakerOgRoller(any()) } returns response

        testApplication {
            environment {
                config = hoconApplicationConfig
            }
            val httpClient = createHttpClient()
            val actualResponse =
                httpClient.post("api/grunnlag/person/roller") {
                    setBody(FoedselsnummerDTO(SOEKER_FOEDSELSNUMMER.value))
                    headers {
                        append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        append(HttpHeaders.Authorization, "Bearer $systembrukerToken")
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
        val response: Set<Long> = setOf(1, 2, 3)
        every { grunnlagService.hentAlleSakerForFnr(any()) } returns response

        testApplication {
            environment {
                config = hoconApplicationConfig
            }
            val httpClient = createHttpClient()
            val actualResponse =
                httpClient.post("api/grunnlag/person/saker") {
                    contentType(ContentType.Application.Json)
                    setBody(FoedselsnummerDTO(SOEKER_FOEDSELSNUMMER.value))
                    headers {
                        append(HttpHeaders.Authorization, "Bearer $systembrukerToken")
                    }
                }

            assertEquals(HttpStatusCode.OK, actualResponse.status)
            assertEquals(serialize(response), actualResponse.body<String>())
        }

        verify(exactly = 1) { grunnlagService.hentAlleSakerForFnr(any()) }
        coVerify { behandlingKlient wasNot Called }
    }

    @Test
    fun `Hent navn på person`() {
        val response = NavnOpplysningDTO(1, "Test", "Mellom", "Testesen", SOEKER_FOEDSELSNUMMER.value)
        every { grunnlagService.hentOpplysningstypeNavnFraFnr(any(), any()) } returns response
        coEvery { behandlingKlient.harTilgangTilPerson(any(), any()) } returns true

        testApplication {
            environment {
                config = hoconApplicationConfig
            }
            val httpClient = createHttpClient()
            val actualResponse =
                httpClient.post("api/grunnlag/person/navn") {
                    contentType(ContentType.Application.Json)
                    setBody(FoedselsnummerDTO(SOEKER_FOEDSELSNUMMER.value))
                    headers {
                        append(HttpHeaders.Authorization, "Bearer $saksbehandlerToken")
                    }
                }

            assertEquals(HttpStatusCode.OK, actualResponse.status)
            assertEquals(serialize(response), actualResponse.body<String>())
        }

        verify(exactly = 1) { grunnlagService.hentOpplysningstypeNavnFraFnr(SOEKER_FOEDSELSNUMMER, any()) }
        coVerify { behandlingKlient.harTilgangTilPerson(SOEKER_FOEDSELSNUMMER, any()) }
    }

    @Test
    fun `Hent navn på person - saksbehandler har ikke tilgang`() {
        coEvery { behandlingKlient.harTilgangTilPerson(any(), any()) } returns false

        testApplication {
            environment {
                config = hoconApplicationConfig
            }
            val httpClient = createHttpClient()
            val actualResponse =
                httpClient.post("api/grunnlag/person/navn") {
                    contentType(ContentType.Application.Json)
                    setBody(FoedselsnummerDTO(SOEKER_FOEDSELSNUMMER.value))
                    headers {
                        append(HttpHeaders.Authorization, "Bearer $saksbehandlerToken")
                    }
                }

            assertEquals(HttpStatusCode.NotFound, actualResponse.status)
        }

        coVerify { behandlingKlient.harTilgangTilPerson(SOEKER_FOEDSELSNUMMER, any()) }
    }

    private fun ApplicationTestBuilder.createHttpClient(): HttpClient {
        application {
            restModule(this.log, routePrefix = "api/grunnlag") { personRoute(grunnlagService, behandlingKlient) }
        }

        return createClient {
            install(ContentNegotiation) {
                jackson { registerModule(JavaTimeModule()) }
            }
        }
    }
}
