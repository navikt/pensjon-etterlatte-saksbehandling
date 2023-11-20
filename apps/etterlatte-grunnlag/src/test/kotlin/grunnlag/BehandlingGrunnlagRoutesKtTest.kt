package no.nav.etterlatte.grunnlag

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.HttpClient
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
import io.ktor.server.testing.ApplicationTestBuilder
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
import no.nav.etterlatte.grunnlag.klienter.BehandlingKlient
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.NyeSaksopplysninger
import no.nav.etterlatte.libs.common.grunnlag.OppdaterGrunnlagRequest
import no.nav.etterlatte.libs.common.grunnlag.Opplysningsbehov
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.serialize
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.ktor.AZURE_ISSUER
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import testsupport.buildTestApplicationConfigurationForOauth
import java.util.UUID
import kotlin.random.Random

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BehandlingGrunnlagRoutesKtTest {
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
        val behandlingId = UUID.randomUUID()

        testApplication {
            val response = createHttpClient().get("api/grunnlag/behandling/$behandlingId")

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

        coVerify(exactly = 1) { behandlingKlient wasNot Called }
    }

    @Test
    fun `returnerer 404 hvis grunnlag ikke finnes`() {
        val behandlingId = UUID.randomUUID()

        every { grunnlagService.hentOpplysningsgrunnlag(any()) } returns null
        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any()) } returns true

        testApplication {
            environment {
                config = hoconApplicationConfig
            }
            application {
                restModule(this.log, routePrefix = "api/grunnlag") {
                    behandlingGrunnlagRoute(
                        grunnlagService,
                        behandlingKlient,
                    )
                }
            }
            val response =
                client.get("api/grunnlag/behandling/$behandlingId") {
                    headers {
                        append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        append(HttpHeaders.Authorization, "Bearer $token")
                    }
                }

            assertEquals(HttpStatusCode.NotFound, response.status)
        }

        verify(exactly = 1) { grunnlagService.hentOpplysningsgrunnlag(any()) }
        coVerify(exactly = 1) { behandlingKlient.harTilgangTilBehandling(any(), any()) }
    }

    @Test
    fun `Hent grunnlag for behandling`() {
        val behandlingId = UUID.randomUUID()
        val testData = GrunnlagTestData().hentOpplysningsgrunnlag()

        every { grunnlagService.hentOpplysningsgrunnlag(any<UUID>()) } returns testData
        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any()) } returns true

        testApplication {
            val response =
                createHttpClient().get("api/grunnlag/behandling/$behandlingId") {
                    headers {
                        append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        append(HttpHeaders.Authorization, "Bearer $token")
                    }
                }

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(serialize(testData), response.body<String>())
        }

        verify(exactly = 1) { grunnlagService.hentOpplysningsgrunnlag(any()) }
        coVerify(exactly = 1) { behandlingKlient.harTilgangTilBehandling(any(), any()) }
    }

    @ParameterizedTest
    @EnumSource(Opplysningstype::class)
    fun `Hent grunnlag av type for behandling`(type: Opplysningstype) {
        val behandlingId = UUID.randomUUID()

        val opplysning =
            Grunnlagsopplysning(
                id = UUID.randomUUID(),
                kilde = Grunnlagsopplysning.Pdl(Tidspunkt.now(), null, null),
                opplysningType = type,
                meta = objectMapper.createObjectNode(),
                opplysning = """{}""".toJsonNode(),
            )

        every { grunnlagService.hentGrunnlagAvType(any<UUID>(), any<Opplysningstype>()) } returns opplysning
        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any()) } returns true

        testApplication {
            val response =
                createHttpClient().get("api/grunnlag/behandling/$behandlingId/$type") {
                    headers {
                        append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        append(HttpHeaders.Authorization, "Bearer $token")
                    }
                }

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(serialize(opplysning), response.body<String>())
        }

        verify(exactly = 1) { grunnlagService.hentGrunnlagAvType(behandlingId, type) }
        coVerify(exactly = 1) { behandlingKlient.harTilgangTilBehandling(any(), any()) }
    }

    @Test
    fun `Hent historisk foreldreansvar for behandling`() {
        val behandlingId = UUID.randomUUID()

        val opplysning =
            Grunnlagsopplysning(
                id = UUID.randomUUID(),
                kilde = Grunnlagsopplysning.Pdl(Tidspunkt.now(), null, null),
                opplysningType = Opplysningstype.HISTORISK_FORELDREANSVAR,
                meta = objectMapper.createObjectNode(),
                opplysning = """{}""".toJsonNode(),
            )

        every { grunnlagService.hentHistoriskForeldreansvar(any<UUID>()) } returns opplysning
        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any()) } returns true

        testApplication {
            val response =
                createHttpClient().get(
                    "api/grunnlag/behandling/$behandlingId/revurdering/${Opplysningstype.HISTORISK_FORELDREANSVAR}",
                ) {
                    headers {
                        append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        append(HttpHeaders.Authorization, "Bearer $token")
                    }
                }

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(serialize(opplysning), response.body<String>())
        }

        verify(exactly = 1) { grunnlagService.hentHistoriskForeldreansvar(behandlingId) }
        coVerify(exactly = 1) { behandlingKlient.harTilgangTilBehandling(any(), any()) }
    }

    @Test
    fun `Teste endepunkt for lagring av nye saksopplysninger`() {
        val sakId = Random.nextLong()
        val behandlingId = UUID.randomUUID()
        val opplysninger =
            listOf(
                lagGrunnlagsopplysning(
                    opplysningstype = Opplysningstype.SPRAAK,
                    kilde = Grunnlagsopplysning.Privatperson("fnr", Tidspunkt.now()),
                    verdi = "nb".toJsonNode(),
                ),
            )

        every { grunnlagService.lagreNyeSaksopplysninger(any(), any(), any()) } just Runs
        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any()) } returns true

        testApplication {
            val actualResponse =
                createHttpClient().post("api/grunnlag/behandling/$behandlingId/nye-opplysninger") {
                    contentType(ContentType.Application.Json)
                    setBody(NyeSaksopplysninger(sakId, opplysninger))
                    headers {
                        append(HttpHeaders.Authorization, "Bearer $token")
                    }
                }

            assertEquals(HttpStatusCode.OK, actualResponse.status)
        }

        val opplysningerSlot = slot<List<Grunnlagsopplysning<JsonNode>>>()

        verify(exactly = 1) { grunnlagService.lagreNyeSaksopplysninger(sakId, behandlingId, capture(opplysningerSlot)) }
        coVerify(exactly = 1) { behandlingKlient.harTilgangTilBehandling(behandlingId, any()) }

        val faktiskOpplysning = opplysningerSlot.captured.single()

        assertEquals(serialize(opplysninger.single()), serialize(faktiskOpplysning))
    }

    @Test
    fun `Teste endepunkt for oppretting av grunnlag`() {
        val sakId = 12345L
        val behandlingId = UUID.randomUUID()
        val persongalleri = GrunnlagTestData().hentPersonGalleri()
        val opplysningsbehov = Opplysningsbehov(sakId, SakType.BARNEPENSJON, persongalleri)

        coEvery { grunnlagService.opprettGrunnlag(any(), any()) } just Runs

        testApplication {
            val actualResponse =
                createHttpClient().post("api/grunnlag/behandling/$behandlingId/opprett-grunnlag") {
                    contentType(ContentType.Application.Json)
                    setBody(opplysningsbehov)
                    headers {
                        append(HttpHeaders.Authorization, "Bearer $systemBruker")
                    }
                }

            assertEquals(HttpStatusCode.OK, actualResponse.status)
        }

        val behovSlot = slot<Opplysningsbehov>()
        coVerify(exactly = 1) {
            grunnlagService.opprettGrunnlag(
                behandlingId,
                capture(behovSlot),
            )
        }
        coVerify { behandlingKlient wasNot Called }

        assertEquals(opplysningsbehov, behovSlot.captured)
    }

    @Test
    fun `Teste endepunkt for oppdatering av grunnlag`() {
        val sakId = 12345L
        val behandlingId = UUID.randomUUID()

        coEvery { grunnlagService.oppdaterGrunnlag(any(), any(), any()) } just Runs

        val request = OppdaterGrunnlagRequest(sakId, SakType.BARNEPENSJON)
        testApplication {
            val actualResponse =
                createHttpClient().post("api/grunnlag/behandling/$behandlingId/oppdater-grunnlag") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                    headers {
                        append(HttpHeaders.Authorization, "Bearer $systemBruker")
                    }
                }

            assertEquals(HttpStatusCode.OK, actualResponse.status)
        }

        coVerify(exactly = 1) {
            grunnlagService.oppdaterGrunnlag(
                behandlingId,
                sakId,
                SakType.BARNEPENSJON,
            )
        }
        coVerify { behandlingKlient wasNot Called }
    }

    private fun ApplicationTestBuilder.createHttpClient(): HttpClient {
        environment {
            config = hoconApplicationConfig
        }
        application {
            restModule(this.log, routePrefix = "api/grunnlag") {
                behandlingGrunnlagRoute(grunnlagService, behandlingKlient)
            }
        }

        return createClient {
            install(ContentNegotiation) {
                jackson { registerModule(JavaTimeModule()) }
            }
        }
    }
}
