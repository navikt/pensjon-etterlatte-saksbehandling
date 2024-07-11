package no.nav.etterlatte.samordning.vedtak

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.testing.testApplication
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.etterlatte.libs.ktor.route.routeLogger
import no.nav.etterlatte.libs.ktor.token.Claims
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SamordningVedtakRouteTest {
    private val server = MockOAuth2Server()
    private val samordningVedtakService = mockk<SamordningVedtakService>()
    private lateinit var config: Config
    private lateinit var applicationConfig: HoconApplicationConfig

    @BeforeAll
    fun before() {
        server.start()
    }

    @Nested
    inner class MaskinportenApi {
        @BeforeEach
        fun before() {
            config = config(server.config.httpServer.port(), ISSUER_ID_MASKINPORTEN)
            applicationConfig = HoconApplicationConfig(config)
        }

        @Test
        fun `skal gi 401 naar token mangler`() {
            testApplication {
                environment { config = applicationConfig }
                application { samordningVedtakApi() }

                val response =
                    client.get("/api/vedtak/123") {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header("tpnr", "3010")
                    }

                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        @Test
        fun `skal gi 401 med token hvor scope mangler`() {
            testApplication {
                environment { config = applicationConfig }
                application { samordningVedtakApi() }

                val response =
                    client.get("/api/vedtak/123") {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(HttpHeaders.Authorization, "Bearer ${token()}")
                        header("tpnr", "3010")
                    }

                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        @Test
        fun `skal gi 400 dersom tpnr-header mangler`() {
            testApplication {
                environment { config = applicationConfig }
                application { samordningVedtakApi() }

                val response =
                    client.get("/api/vedtak/123") {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(
                            HttpHeaders.Authorization,
                            "Bearer ${token("nav:etterlatteytelser:vedtaksinformasjon.read")}",
                        )
                    }

                response.status shouldBe HttpStatusCode.BadRequest
            }
        }

        @Test
        fun `skal gi 200 med gyldig token inkl scope`() {
            coEvery {
                samordningVedtakService.hentVedtak(
                    vedtakId = any<Long>(),
                    MaskinportenTpContext(tpnr = Tjenestepensjonnummer("3010"), organisasjonsnr = "0123456789"),
                )
            } returns
                opprettSamordningVedtakDto()

            testApplication {
                environment { config = applicationConfig }
                application { samordningVedtakApi() }

                val response =
                    client.get("/api/vedtak/123") {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(
                            HttpHeaders.Authorization,
                            "Bearer ${token("nav:etterlatteytelser:vedtaksinformasjon.read")}",
                        )
                        header("tpnr", "3010")
                    }

                response.status shouldBe HttpStatusCode.OK
                coVerify { samordningVedtakService.hentVedtak(vedtakId = any<Long>(), any<MaskinportenTpContext>()) }
            }
        }

        @Test
        fun `skal gi 200 med gyldig token inkl scope - hent vedtaksliste med virkfom og fnr`() {
            val virkFom = LocalDate.now()
            val fnr = "01448203510"

            coEvery {
                samordningVedtakService.hentVedtaksliste(
                    fomDato = virkFom,
                    fnr = Folkeregisteridentifikator.of(fnr),
                    context =
                        MaskinportenTpContext(
                            tpnr = Tjenestepensjonnummer("3010"),
                            organisasjonsnr = "0123456789",
                        ),
                )
            } returns
                listOf(opprettSamordningVedtakDto())

            testApplication {
                environment { config = applicationConfig }
                application { samordningVedtakApi() }

                val response =
                    client.get("/api/vedtak") {
                        parameter("fomDato", virkFom)
                        header("fnr", fnr)
                        header("tpnr", "3010")
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(
                            HttpHeaders.Authorization,
                            "Bearer ${token("nav:etterlatteytelser:vedtaksinformasjon.read")}",
                        )
                    }

                response.status shouldBe HttpStatusCode.OK
                coVerify {
                    samordningVedtakService.hentVedtaksliste(
                        fomDato = virkFom,
                        fnr = Folkeregisteridentifikator.of(fnr),
                        context = any<MaskinportenTpContext>(),
                    )
                }
            }
        }

        private fun token(maskinportenScope: String? = null): String {
            val claims = mutableMapOf<String, Any>()
            claims["consumer"] = mapOf("ID" to "0192:0123456789")
            maskinportenScope?.let { claims["scope"] = it }

            return server
                .issueToken(
                    issuerId = ISSUER_ID_MASKINPORTEN,
                    claims = claims,
                ).serialize()
        }
    }

    @Nested
    inner class PensjonApi {
        private val virkFom = LocalDate.now()
        private val fnr = "01448203510"

        @BeforeEach
        fun before() {
            config = config(server.config.httpServer.port(), ISSUER_ID_AZURE)
            applicationConfig = HoconApplicationConfig(config)
        }

        @Test
        fun `skal gi 401 naar token mangler`() {
            testApplication {
                environment { config = applicationConfig }
                application { samordningVedtakApi() }

                val response =
                    client.get("/api/pensjon/vedtak") {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        parameter("fomDato", virkFom)
                        header("fnr", fnr)
                    }

                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        @Test
        fun `skal gi 401 med token hvor rolle mangler`() {
            testApplication {
                environment { config = applicationConfig }
                application { samordningVedtakApi() }

                val response =
                    client.get("/api/pensjon/vedtak") {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(HttpHeaders.Authorization, "Bearer ${token()}")
                        parameter("fomDato", virkFom)
                        header("fnr", fnr)
                    }

                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        @Test
        fun `skal gi 200 med gyldig token inkl rolle`() {
            coEvery {
                samordningVedtakService.hentVedtaksliste(
                    fomDato = virkFom,
                    fnr = Folkeregisteridentifikator.of(fnr),
                    context = PensjonContext,
                )
            } returns
                listOf(opprettSamordningVedtakDto())

            testApplication {
                environment { config = applicationConfig }
                application { samordningVedtakApi() }

                val response =
                    client.get("/api/pensjon/vedtak") {
                        parameter("fomDato", virkFom)
                        header("fnr", fnr)
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(HttpHeaders.Authorization, "Bearer ${token("les-oms-vedtak")}")
                    }

                response.status shouldBe HttpStatusCode.OK
                coVerify {
                    samordningVedtakService.hentVedtaksliste(
                        fomDato = virkFom,
                        fnr = Folkeregisteridentifikator.of(fnr),
                        context = PensjonContext,
                    )
                }
            }
        }

        private fun token(role: String? = null): String {
            val claims = mutableMapOf<String, Any>()
            claims["roles"] = listOf(role)
            claims[Claims.idtyp.name] = "app"

            return server
                .issueToken(
                    issuerId = ISSUER_ID_AZURE,
                    claims = claims,
                ).serialize()
        }
    }

    private fun Application.samordningVedtakApi() {
        restModule(routeLogger) {
            samordningVedtakRoute(
                samordningVedtakService = samordningVedtakService,
                config = config,
            )
        }
    }

    @AfterEach
    fun afterEach() {
        confirmVerified()
        clearAllMocks()
    }

    @AfterAll
    fun after() {
        server.shutdown()
    }

    companion object {
        const val ISSUER_ID_MASKINPORTEN = "maskinporten"
        const val ISSUER_ID_AZURE = "azure"
    }
}

private fun config(
    port: Int,
    issuerId: String,
) = ConfigFactory.parseMap(
    mapOf(
        "no.nav.security.jwt.issuers" to
            listOf(
                mapOf(
                    "discoveryurl" to "http://localhost:$port/$issuerId/.well-known/openid-configuration",
                    "issuer_name" to issuerId,
                    "validation.optional_claims" to "aud,nbf,sub",
                ),
            ),
        "roller" to
            mapOf(
                "pensjon-saksbehandler" to UUID.randomUUID().toString(),
            ),
    ),
)

fun opprettSamordningVedtakDto() =
    SamordningVedtakDto(
        vedtakId = 1L,
        sakstype = "OMS",
        virkningsdato = LocalDate.now().withDayOfMonth(1),
        opphoersdato = null,
        type = SamordningVedtakType.START,
        anvendtTrygdetid = 40,
        aarsak = null,
        perioder = emptyList(),
    )
