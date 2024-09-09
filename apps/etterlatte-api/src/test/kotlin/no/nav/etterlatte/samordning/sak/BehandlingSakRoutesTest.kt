package no.nav.etterlatte.samordning.sak

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.testing.testApplication
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import no.nav.etterlatte.ktor.startRandomPort
import no.nav.etterlatte.ktor.token.CLIENT_ID
import no.nav.etterlatte.ktor.token.issueSaksbehandlerToken
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.etterlatte.libs.ktor.route.FoedselsnummerDTO
import no.nav.etterlatte.libs.ktor.route.routeLogger
import no.nav.etterlatte.libs.ktor.token.Issuer
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BehandlingSakRoutesTest {
    private val mockOAuth2Server = MockOAuth2Server()
    private val behandlingService = mockk<BehandlingService>()
    private lateinit var applicationConfig: HoconApplicationConfig

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
        val conff = config(mockOAuth2Server.config.httpServer.port(), Issuer.AZURE.issuerName)
        applicationConfig = HoconApplicationConfig(conff)
        testApplication {
            environment { config = applicationConfig }
            application {
                restModule(routeLogger) {
                    behandlingSakRoutes(
                        behandlingService = behandlingService,
                        config = conff,
                    )
                }
            }

            val response =
                client.post("api/oms/person/sak") {
                    contentType(ContentType.Application.Json)
                    setBody(FoedselsnummerDTO(fnr).toJson())
                }
            response.status shouldBe HttpStatusCode.Unauthorized
            coVerify(exactly = 0) { behandlingService.hentSakforPerson(any()) }
        }
    }

    @Test
    fun `skal gi 401 når rolle mangler`() {
        val conff = config(mockOAuth2Server.config.httpServer.port(), Issuer.AZURE.issuerName)
        applicationConfig = HoconApplicationConfig(conff)
        testApplication {
            environment { config = applicationConfig }
            application {
                restModule(routeLogger) {
                    behandlingSakRoutes(
                        behandlingService = behandlingService,
                        config = conff,
                    )
                }
            }

            val response =
                client.post("api/oms/person/sak") {
                    contentType(ContentType.Application.Json)
                    setBody(FoedselsnummerDTO(fnr).toJson())
                    header(HttpHeaders.Authorization, "Bearer ${mockOAuth2Server.issueSaksbehandlerToken()}")
                }
            response.status shouldBe HttpStatusCode.Unauthorized
            coVerify(exactly = 0) { behandlingService.hentSakforPerson(any()) }
        }
    }

    @Test
    fun `skal gi 500 når body mangler rolle les-oms-sak-for-person(kun dev)`() {
        val conff = config(mockOAuth2Server.config.httpServer.port(), Issuer.AZURE.issuerName)
        applicationConfig = HoconApplicationConfig(conff)
        testApplication {
            environment { config = applicationConfig }
            application {
                restModule(routeLogger) {
                    behandlingSakRoutes(
                        behandlingService = behandlingService,
                        config = conff,
                    )
                }
            }

            val response =
                client.post("api/oms/person/sak") {
                    contentType(ContentType.Application.Json)
                    header(
                        HttpHeaders.Authorization,
                        "Bearer ${mockOAuth2Server.issueSaksbehandlerToken(groups = listOf("les-oms-sak-for-person"))}",
                    )
                }
            response.status shouldBe HttpStatusCode.InternalServerError
            coVerify(exactly = 0) { behandlingService.hentSakforPerson(any()) }
        }
    }

    @Test
    fun `skal gi 500 når body mangler pensjonSaksbehandler`() {
        val pensjonSaksbehandler = UUID.randomUUID().toString()
        val conff = config(mockOAuth2Server.config.httpServer.port(), Issuer.AZURE.issuerName, pensjonSaksbehandler = pensjonSaksbehandler)
        applicationConfig = HoconApplicationConfig(conff)
        testApplication {
            environment { config = applicationConfig }
            application {
                restModule(routeLogger) {
                    behandlingSakRoutes(
                        behandlingService = behandlingService,
                        config = conff,
                    )
                }
            }

            val response =
                client.post("api/oms/person/sak") {
                    contentType(ContentType.Application.Json)
                    header(
                        HttpHeaders.Authorization,
                        "Bearer ${mockOAuth2Server.issueSaksbehandlerToken(groups = listOf(pensjonSaksbehandler))}",
                    )
                }
            response.status shouldBe HttpStatusCode.InternalServerError
            coVerify(exactly = 0) { behandlingService.hentSakforPerson(any()) }
        }
    }

    @Test
    fun `pensjonSaksbehandler kan hente saksliste for fnr`() {
        val pensjonSaksbehandler = UUID.randomUUID().toString()
        val conff = config(mockOAuth2Server.config.httpServer.port(), Issuer.AZURE.issuerName, pensjonSaksbehandler = pensjonSaksbehandler)
        applicationConfig = HoconApplicationConfig(conff)
        val requestFnr = FoedselsnummerDTO(fnr)
        val sakIdListesvar = listOf(1L)
        coEvery { behandlingService.hentSakforPerson(requestFnr) } returns sakIdListesvar
        testApplication {
            environment { config = applicationConfig }
            application {
                restModule(routeLogger) {
                    behandlingSakRoutes(
                        behandlingService = behandlingService,
                        config = conff,
                    )
                }
            }
            val client =
                createClient {
                    install(ContentNegotiation) {
                        register(ContentType.Application.Json, JacksonConverter(objectMapper))
                    }
                }

            val response =
                client.post("api/oms/person/sak") {
                    contentType(ContentType.Application.Json)
                    setBody(requestFnr.toJson())
                    header(
                        HttpHeaders.Authorization,
                        "Bearer ${mockOAuth2Server.issueSaksbehandlerToken(groups = listOf(pensjonSaksbehandler))}",
                    )
                }
            response.status shouldBe HttpStatusCode.OK
            println(response.bodyAsText())
            val sakliste: List<Long> = response.body()

            sakliste shouldBe sakIdListesvar

            coVerify(exactly = 1) { behandlingService.hentSakforPerson(requestFnr) }
        }
    }
}

private fun config(
    port: Int,
    issuerId: String,
    pensjonSaksbehandler: String? = UUID.randomUUID().toString(),
    gjennySaksbehandler: String? = UUID.randomUUID().toString(),
): Config =
    ConfigFactory.parseMap(
        mapOf(
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
