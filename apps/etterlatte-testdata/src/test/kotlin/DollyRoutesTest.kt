package no.nav.etterlatte.dolly

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.utils.EmptyContent.contentType
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import no.nav.etterlatte.config
import no.nav.etterlatte.ktor.runServerWithConfig
import no.nav.etterlatte.ktor.startRandomPort
import no.nav.etterlatte.ktor.token.CLIENT_ID
import no.nav.etterlatte.ktor.token.issueSaksbehandlerToken
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.SoeknadType
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.ktor.route.FoedselsnummerDTO
import no.nav.etterlatte.libs.ktor.token.Issuer
import no.nav.etterlatte.testdata.dolly.DollyService
import no.nav.etterlatte.testdata.features.dolly.DollyFeature
import no.nav.etterlatte.testdata.features.dolly.NySoeknadRequest
import no.nav.etterlatte.testdata.features.dolly.SoeknadResponse
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DollyRoutesTest {
    private val mockOAuth2Server =
        MockOAuth2Server()
    private val dollyService: DollyService = mockk<DollyService>()

    @BeforeAll
    fun before() {
        mockOAuth2Server.startRandomPort()
    }

    @AfterAll
    fun after() {
        mockOAuth2Server.shutdown()
    }

    @AfterEach
    fun afterEach() {
        confirmVerified()
        clearAllMocks()
    }

    val fnr = "01448203510"

    @Test
    fun `skal gi 401 naar token mangler`() {
        val conff =
            configMedRoller(
                mockOAuth2Server.config.httpServer.port(),
                Issuer.AZURE.issuerName,
            )
        testApplication {
            runServerWithConfig(applicationConfig = conff, routes = DollyFeature(dollyService = dollyService).routes)

            val response =
                client.post("/opprett-ytelse") {
                    contentType(ContentType.Application.Json)
                    setBody(FoedselsnummerDTO(fnr).toJson())
                }
            response.status shouldBe HttpStatusCode.Unauthorized
        }
    }

    @Test
    fun `skal gi 400 når body mangler `() {
        val pensjonSaksbehandler = UUID.randomUUID().toString()
        val conff =
            configMedRoller(
                mockOAuth2Server.config.httpServer.port(),
                Issuer.AZURE.issuerName,
                pensjonSaksbehandler = pensjonSaksbehandler,
            )
        testApplication {
            runServerWithConfig(applicationConfig = conff, routes = DollyFeature(dollyService = dollyService).routes)

            val response =
                client.post("/opprett-ytelse") {
                    contentType(ContentType.Application.Json)
                    // setBody(FoedselsnummerDTO(fnr).toJson())
                    header(
                        HttpHeaders.Authorization,
                        "Bearer ${mockOAuth2Server.issueSaksbehandlerToken(groups = listOf(pensjonSaksbehandler))}",
                    )
                }
            response.status shouldBe HttpStatusCode.BadRequest
        }
    }

    @Test
    fun `kan opprette soeknad`() {
        val pensjonSaksbehandler = UUID.randomUUID().toString()
        val conff =
            configMedRoller(
                mockOAuth2Server.config.httpServer.port(),
                Issuer.AZURE.issuerName,
                pensjonSaksbehandler = pensjonSaksbehandler,
            )
        val request =
            NySoeknadRequest(
                type = SoeknadType.BARNEPENSJON,
                avdoed = fnr,
                gjenlevende = fnr,
                barn =
                    listOf(
                        fnr,
                        fnr,
                    ),
            )
        coEvery { dollyService.sendSoeknad(any(), any(), any()) } returns "1"

        testApplication {
            val client =
                runServerWithConfig(applicationConfig = conff, routes = DollyFeature(dollyService = dollyService).routes)

            val response =
                client.post("/opprett-ytelse") {
                    contentType(ContentType.Application.Json)
                    setBody(request.toJson())
                    header(
                        HttpHeaders.Authorization,
                        "Bearer ${mockOAuth2Server.issueSaksbehandlerToken(groups = listOf(pensjonSaksbehandler))}",
                    )
                }
            response.status shouldBe HttpStatusCode.OK
            val resultat: SoeknadResponse = response.body()

            resultat shouldBe SoeknadResponse(200, resultat.noekkel)

            coVerify(exactly = 1) { dollyService.sendSoeknad(any(), any(), any()) }
        }
    }

    private fun configMedRoller(
        port: Int,
        issuerId: String,
        pensjonSaksbehandler: String? = UUID.randomUUID().toString(),
        gjennySaksbehandler: String? = UUID.randomUUID().toString(),
    ): Config =
        ConfigFactory.parseMap(
            mapOf(
                "testnav.resource.url" to "http://localhost",
                "no.nav.security.jwt.issuers" to
                    listOf(
                        mapOf(
                            "discoveryurl" to "http://localhost:$port/$issuerId/.well-known/openid-configuration",
                            "issuer_name" to issuerId,
                            "accepted_audience" to CLIENT_ID,
                        ),
                    ),
                "roller" to
                    mapOf(
                        "pensjon-saksbehandler" to pensjonSaksbehandler,
                        "gjenny-saksbehandler" to gjennySaksbehandler,
                    ),
            ),
        )
}
