package no.nav.etterlatte.samordning.vedtak

import com.typesafe.config.ConfigFactory
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.log
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.testing.testApplication
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.etterlatte.validateMaskinportenScope
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SamordningVedtakRouteTest {
    private val server = MockOAuth2Server()
    private val samordningVedtakService = mockk<SamordningVedtakService>()
    private lateinit var applicationConfig: HoconApplicationConfig

    @BeforeAll
    fun before() {
        server.start()

        applicationConfig =
            buildTestApplicationConfigurationForOauth(server.config.httpServer.port(), ISSUER_ID)
    }

    @Test
    fun `skal gi 401 naar token mangler`() {
        testApplication {
            environment { config = applicationConfig }
            application { samordningVedtakApi() }

            val response =
                client.get("/api/vedtak/123") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
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
                }

            response.status shouldBe HttpStatusCode.Unauthorized
        }
    }

    @Test
    fun `skal gi 200 med gyldig token inkl scope`() {
        coEvery { samordningVedtakService.hentVedtak(any<Long>(), any<String>()) } returns
            mockk<SamordningVedtakDto>()

        testApplication {
            environment { config = applicationConfig }
            application { samordningVedtakApi() }

            val response =
                client.get("/api/vedtak/123") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(
                        HttpHeaders.Authorization,
                        "Bearer ${token("nav:etterlatteytelser:vedtaksinformasjon.read")}"
                    )
                }

            response.status shouldBe HttpStatusCode.OK
            coVerify { samordningVedtakService.hentVedtak(any<Long>(), any<String>()) }
        }
    }

    private fun Application.samordningVedtakApi() {
        restModule(
            log,
            additionalValidation = validateMaskinportenScope()
        ) { samordningVedtakRoute(samordningVedtakService = samordningVedtakService) }
    }

    private fun token(maskinportenScope: String? = null): String {
        val claims = mutableMapOf<String, Any>()
        claims.put("consumer", mapOf("ID" to "0192:0123456789"))
        maskinportenScope?.let { claims.put("scope", maskinportenScope) }

        return server.issueToken(
            issuerId = ISSUER_ID,
            claims = claims
        ).serialize()
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
        const val ISSUER_ID = "maskinporten"
    }
}

fun buildTestApplicationConfigurationForOauth(
    port: Int,
    issuerId: String
) = HoconApplicationConfig(
    ConfigFactory.parseMap(
        mapOf(
            "no.nav.security.jwt.issuers" to
                listOf(
                    mapOf(
                        "discoveryurl" to "http://localhost:$port/$issuerId/.well-known/openid-configuration",
                        "issuer_name" to issuerId,
                        "validation.optional_claims" to "aud,nbf,sub"
                    )
                )
        )
    )
)