package no.nav.etterlatte.dolly

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.utils.EmptyContent.contentType
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import io.mockk.clearAllMocks
import io.mockk.confirmVerified
import io.mockk.mockk
import no.nav.etterlatte.config
import no.nav.etterlatte.ktor.runServerWithConfig
import no.nav.etterlatte.ktor.startRandomPort
import no.nav.etterlatte.ktor.token.CLIENT_ID
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.ktor.route.FoedselsnummerDTO
import no.nav.etterlatte.libs.ktor.token.Issuer
import no.nav.etterlatte.testdata.dolly.DollyService
import no.nav.etterlatte.testdata.features.dolly.DollyFeature
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DollyRoutesTest {
    private val mockOAuth2Server =
        no.nav.security.mock.oauth2
            .MockOAuth2Server()
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

/*
    @Test
    fun `skal gi 401 når rolle mangler`() {
        val conff = no.nav.etterlatte.behandling.sak.configMedRoller(
            mockOAuth2Server.config.httpServer.port(),
            Issuer.AZURE.issuerName
        )
        io.ktor.server.testing.testApplication {
            runServerWithConfig(applicationConfig = conff) {
                behandlingSakRoutes(
                    behandlingService = behandlingService,
                    config = conff,
                )
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
    fun `skal gi 500 når body mangler pensjonSaksbehandler`() {
        val pensjonSaksbehandler = UUID.randomUUID().toString()
        val conff =
            no.nav.etterlatte.behandling.sak.configMedRoller(
                mockOAuth2Server.config.httpServer.port(),
                Issuer.AZURE.issuerName,
                pensjonSaksbehandler = pensjonSaksbehandler
            )
        io.ktor.server.testing.testApplication {
            runServerWithConfig(applicationConfig = conff) {
                behandlingSakRoutes(
                    behandlingService = behandlingService,
                    config = conff,
                )
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
        val conff =
            no.nav.etterlatte.behandling.sak.configMedRoller(
                mockOAuth2Server.config.httpServer.port(),
                Issuer.AZURE.issuerName,
                pensjonSaksbehandler = pensjonSaksbehandler
            )
        val requestFnr = FoedselsnummerDTO(fnr)
        val sakIdListesvar = listOf(no.nav.etterlatte.behandling.sakId1)
        coEvery { behandlingService.hentSakforPerson(requestFnr) } returns sakIdListesvar
        io.ktor.server.testing.testApplication {
            val client =
                runServerWithConfig(applicationConfig = conff) {
                    behandlingSakRoutes(
                        behandlingService = behandlingService,
                        config = conff,
                    )
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
            val sakliste: List<SakId> = response.body()

            sakliste shouldBe sakIdListesvar

            coVerify(exactly = 1) { behandlingService.hentSakforPerson(requestFnr) }
        }
    }

    @Test
    fun `Kan hente sak men sak er null og kaster da exception IkkeFunnetException men logges `() {
        val pensjonSaksbehandler = UUID.randomUUID().toString()
        val conff =
            no.nav.etterlatte.behandling.sak.configMedRoller(
                mockOAuth2Server.config.httpServer.port(),
                Issuer.AZURE.issuerName,
                pensjonSaksbehandler = pensjonSaksbehandler
            )
        coEvery { behandlingService.hentSak(any()) } returns null
        io.ktor.server.testing.testApplication {
            runServerWithConfig(applicationConfig = conff) {
                behandlingSakRoutes(
                    behandlingService = behandlingService,
                    config = conff,
                )
            }
            val client =
                createClient {
                    install(ContentNegotiation) {
                        register(ContentType.Application.Json, JacksonConverter(objectMapper))
                    }
                }

            val response =
                client.get("/api/sak/25895819") {
                    contentType(ContentType.Application.Json)
                    header(
                        HttpHeaders.Authorization,
                        "Bearer ${mockOAuth2Server.issueSaksbehandlerToken(groups = listOf("les-oms-sak"))}",
                    )
                }
            response.status shouldBe HttpStatusCode.NotFound
            val feil: ExceptionResponse = response.body()
            feil.code shouldBe "SAK_IKKE_FUNNET"
            feil.status shouldBe HttpStatusCode.NotFound.value

            coVerify(exactly = 1) { behandlingService.hentSak(any()) }
        }
    }

    @Test
    fun `Kan hente sak, verifiserer at den blir returnert`() {
        val pensjonSaksbehandler = UUID.randomUUID().toString()
        val conff =
            no.nav.etterlatte.behandling.sak.configMedRoller(
                mockOAuth2Server.config.httpServer.port(),
                Issuer.AZURE.issuerName,
                pensjonSaksbehandler = pensjonSaksbehandler
            )
        val sakId: Long = 12
        val funnetSak =
            Sak(
                "ident",
                SakType.OMSTILLINGSSTOENAD,
                SakId(sakId),
                Enhetsnummer(
                    "4808",
                ),
            )
        coEvery { behandlingService.hentSak(any()) } returns funnetSak
        io.ktor.server.testing.testApplication {
            val client =
                runServerWithConfig(applicationConfig = conff) {
                    behandlingSakRoutes(
                        behandlingService = behandlingService,
                        config = conff,
                    )
                }

            val response =
                client.get("/api/sak/$sakId") {
                    contentType(ContentType.Application.Json)
                    header(
                        HttpHeaders.Authorization,
                        "Bearer ${mockOAuth2Server.issueSaksbehandlerToken(groups = listOf("les-oms-sak"))}",
                    )
                }
            response.status shouldBe HttpStatusCode.OK
            val hentetSak: Sak? = response.body()

            hentetSak shouldBe funnetSak

            coVerify(exactly = 1) { behandlingService.hentSak(SakId(sakId)) }
        }
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
*/
