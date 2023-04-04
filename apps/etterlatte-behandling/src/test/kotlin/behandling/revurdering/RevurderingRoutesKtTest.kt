package behandling.revurdering

import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.behandling.revurdering.OpprettRevurderingRequest
import no.nav.etterlatte.config.BeanFactory
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.ktor.AZURE_ISSUER
import no.nav.etterlatte.module
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import testsupport.buildTestApplicationConfigurationForOauth
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RevurderingRoutesKtTest {
    private val beanFactory: BeanFactory = mockk(relaxed = true)
    private val server: MockOAuth2Server = MockOAuth2Server()
    private lateinit var hoconApplicationConfig: HoconApplicationConfig

    @BeforeAll
    fun before() {
        server.start()
        val httpServer = server.config.httpServer
        hoconApplicationConfig = buildTestApplicationConfigurationForOauth(httpServer.port(), AZURE_ISSUER, CLIENT_ID)
    }

    @AfterAll
    fun after() {
        server.shutdown()
    }

    @Test
    fun `happy case`() {
        testApplication {
            environment {
                config = hoconApplicationConfig
            }
            application {
                module(beanFactory)
            }
            val client = createClient {
                install(ContentNegotiation) {
                    register(ContentType.Application.Json, JacksonConverter(no.nav.etterlatte.libs.common.objectMapper))
                }
            }

            val response = client.post("api/1/revurdering") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(OpprettRevurderingRequest(RevurderingAarsak.REGULERING))
            }

            Assertions.assertEquals(HttpStatusCode.OK, response.status)
        }
    }

    @Test
    fun `returnerer bad request hvis det ikke finnes noen iverksatt behandling tidligere`() {
        every { beanFactory.generellBehandlingService().hentSenestIverksatteBehandling(1) } returns null
        testApplication {
            environment { config = hoconApplicationConfig }
            application { module(beanFactory) }
            val client = createClient {
                install(ContentNegotiation) {
                    register(ContentType.Application.Json, JacksonConverter(no.nav.etterlatte.libs.common.objectMapper))
                }
            }

            val response = client.post("api/1/revurdering") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(OpprettRevurderingRequest(RevurderingAarsak.REGULERING))
            }

            Assertions.assertEquals(HttpStatusCode.BadRequest, response.status)
        }
    }

    @Test
    fun `returnerer bad request hvis payloaden er ugyldig`() {
        testApplication {
            environment { config = hoconApplicationConfig }
            application { module(beanFactory) }
            val client = createClient {
                install(ContentNegotiation) {
                    register(ContentType.Application.Json, JacksonConverter(no.nav.etterlatte.libs.common.objectMapper))
                }
            }

            val response = client.post("api/1/revurdering") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody("""{ "aarsak": "foo" }""")
            }

            Assertions.assertEquals(HttpStatusCode.BadRequest, response.status)
        }
    }

    private val token: String by lazy {
        server.issueToken(
            issuerId = AZURE_ISSUER,
            audience = CLIENT_ID,
            claims = mapOf(
                "navn" to "John Doe",
                "NAVident" to "Saksbehandler01"
            )
        ).serialize()
    }

    private companion object {
        const val CLIENT_ID = "mock-client-id"
    }
}