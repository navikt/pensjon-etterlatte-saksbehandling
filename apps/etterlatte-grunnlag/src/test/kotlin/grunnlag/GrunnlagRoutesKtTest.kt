package no.nav.etterlatte.grunnlag

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
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
import io.ktor.server.testing.testApplication
import io.mockk.Called
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import lagGrunnlagsopplysning
import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.libs.common.FoedselsnummerDTO
import no.nav.etterlatte.libs.common.behandling.PersonMedSakerOgRoller
import no.nav.etterlatte.libs.common.behandling.SakOgRolle
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.NyeSaksopplysninger
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.serialize
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.ktor.AZURE_ISSUER
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import testsupport.buildTestApplicationConfigurationForOauth
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GrunnlagRoutesKtTest {
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

    @BeforeEach
    fun beforeEach() {
        coEvery { behandlingKlient.harTilgangTilSak(any(), any()) } returns true
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

    private val token by lazy {
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

    private val systemBruker: String by lazy {
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
                restModule(this.log, routePrefix = "api") { grunnlagRoute(grunnlagService, behandlingKlient) }
            }
            val response = client.get("api/grunnlag/1")

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

        coVerify(exactly = 1) { behandlingKlient wasNot Called }
    }

    @Test
    fun `returnerer 404 hvis grunnlag ikke finnes`() {
        every { grunnlagService.hentOpplysningsgrunnlag(any()) } returns null

        testApplication {
            environment {
                config = hoconApplicationConfig
            }
            application {
                restModule(this.log, routePrefix = "api") { grunnlagRoute(grunnlagService, behandlingKlient) }
            }
            val response =
                client.get("api/grunnlag/1") {
                    headers {
                        append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        append(HttpHeaders.Authorization, "Bearer $token")
                    }
                }

            assertEquals(HttpStatusCode.NotFound, response.status)
        }

        verify(exactly = 1) { grunnlagService.hentOpplysningsgrunnlag(any()) }
        coVerify(exactly = 1) { behandlingKlient.harTilgangTilSak(any(), any()) }
    }

    @Test
    fun `200 ok gir mapped data`() {
        val testData = GrunnlagTestData().hentOpplysningsgrunnlag()
        every { grunnlagService.hentOpplysningsgrunnlag(1) } returns testData

        testApplication {
            environment {
                config = hoconApplicationConfig
            }
            application {
                restModule(this.log, routePrefix = "api") { grunnlagRoute(grunnlagService, behandlingKlient) }
            }
            val response =
                client.get("api/grunnlag/1") {
                    headers {
                        append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        append(HttpHeaders.Authorization, "Bearer $token")
                    }
                }

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(serialize(testData), response.body<String>())
        }

        verify(exactly = 1) { grunnlagService.hentOpplysningsgrunnlag(any()) }
        coVerify(exactly = 1) { behandlingKlient.harTilgangTilSak(any(), any()) }
    }

    @Test
    fun `roller tilknyttet person`() {
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
            application {
                restModule(this.log, routePrefix = "api") { grunnlagRoute(grunnlagService, behandlingKlient) }
            }
            val httpClient =
                createClient {
                    install(ContentNegotiation) {
                        jackson { registerModule(JavaTimeModule()) }
                    }
                }
            val actualResponse =
                httpClient.post("api/grunnlag/person/roller") {
                    setBody(FoedselsnummerDTO(SOEKER_FOEDSELSNUMMER.value))
                    headers {
                        append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        append(HttpHeaders.Authorization, "Bearer $systemBruker")
                    }
                }

            assertEquals(HttpStatusCode.OK, actualResponse.status)
            assertEquals(serialize(response), actualResponse.body<String>())
        }

        verify(exactly = 1) { grunnlagService.hentSakerOgRoller(any()) }
        coVerify { behandlingKlient wasNot Called }
    }

    @Test
    fun `saker tilknyttet person`() {
        val response: Set<Long> = setOf(1, 2, 3)
        every { grunnlagService.hentAlleSakerForFnr(any()) } returns response

        testApplication {
            environment {
                config = hoconApplicationConfig
            }
            application {
                restModule(this.log, routePrefix = "api") { grunnlagRoute(grunnlagService, behandlingKlient) }
            }
            val httpClient =
                createClient {
                    install(ContentNegotiation) {
                        jackson { registerModule(JavaTimeModule()) }
                    }
                }
            val actualResponse =
                httpClient.post("api/grunnlag/person/saker") {
                    contentType(ContentType.Application.Json)
                    setBody(FoedselsnummerDTO(SOEKER_FOEDSELSNUMMER.value))
                    headers {
                        append(HttpHeaders.Authorization, "Bearer $systemBruker")
                    }
                }

            assertEquals(HttpStatusCode.OK, actualResponse.status)
            assertEquals(serialize(response), actualResponse.body<String>())
        }

        verify(exactly = 1) { grunnlagService.hentAlleSakerForFnr(any()) }
        coVerify { behandlingKlient wasNot Called }
    }

    @Test
    fun `Teste endepunkt for lagring av nye saksopplysninger`() {
        val sakId = 12345L
        val opplysninger =
            listOf(
                lagGrunnlagsopplysning(
                    opplysningstype = Opplysningstype.SPRAAK,
                    kilde = Grunnlagsopplysning.Privatperson("fnr", Tidspunkt.now()),
                    verdi = "nb".toJsonNode(),
                ),
            )

        every { grunnlagService.lagreNyeSaksopplysninger(any(), any()) } just Runs

        testApplication {
            environment {
                config = hoconApplicationConfig
            }
            application {
                restModule(this.log, routePrefix = "api") { grunnlagRoute(grunnlagService, behandlingKlient) }
            }
            val httpClient =
                createClient {
                    install(ContentNegotiation) {
                        jackson { registerModule(JavaTimeModule()) }
                    }
                }
            val actualResponse =
                httpClient.post("api/grunnlag/$sakId/nye-opplysninger") {
                    contentType(ContentType.Application.Json)
                    setBody(NyeSaksopplysninger(opplysninger))
                    headers {
                        append(HttpHeaders.Authorization, "Bearer $systemBruker")
                    }
                }

            assertEquals(HttpStatusCode.OK, actualResponse.status)
        }

        val opplysningerSlot = slot<List<Grunnlagsopplysning<JsonNode>>>()

        verify(exactly = 1) { grunnlagService.lagreNyeSaksopplysninger(sakId, capture(opplysningerSlot)) }
        coVerify { behandlingKlient wasNot Called }

        val faktiskOpplysning = opplysningerSlot.captured.single()

        assertEquals(serialize(opplysninger.single()), serialize(faktiskOpplysning))
    }
}
