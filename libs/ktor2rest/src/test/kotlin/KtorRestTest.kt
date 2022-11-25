
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.testing.testApplication
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KtorRestTest {

    private val server = MockOAuth2Server()

    @BeforeAll
    fun beforeAll() {
        server.start()
        System.setProperty("AZURE_APP_WELL_KNOWN_URL", server.wellKnownUrl(ISSUER_ID).toString())
        System.setProperty("AZURE_APP_CLIENT_ID", CLIENT_ID)
    }

    @AfterAll
    fun after() {
        server.shutdown()
    }

    private val token: String by lazy {
        server.issueToken(
            issuerId = ISSUER_ID,
            audience = CLIENT_ID,
            claims = mapOf(
                "navn" to "John Doe",
                "NAVident" to "Saksbehandler01"
            )
        ).serialize()
    }

    @Test
    fun `skal sette opp to endepunkter med autentisering`() {
        testApplication {
            application {
                restModule {
                    testRoute1()
                    testRoute2()
                }
            }

            val response1 = client.get("/api/test") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            val response2 = client.get("/api/test2") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            assertEquals(HttpStatusCode.OK, response1.status)
            assertEquals(HttpStatusCode.OK, response2.status)
        }
    }

    @Test
    fun `skal returnere 500 dersom noe feiler`() {
        testApplication {
            application { restModule { testRoute1() } }

            val response = client.get("/api/test/exceptionhandler") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            assertEquals(HttpStatusCode.InternalServerError, response.status)
        }
    }

    companion object {
        const val CLIENT_ID = "azure-id for saksbehandler"
        const val ISSUER_ID = "azure"
    }
}

fun Route.testRoute1() {
    route("/api/test") {
        get("/") {
            call.respond(HttpStatusCode.OK)
        }
        get("/exceptionhandler") {
            throw RuntimeException("Noe feilet")
        }
    }
}

fun Route.testRoute2() {
    route("/api/test2") {
        get("/") {
            call.respond(HttpStatusCode.OK)
        }
    }
}