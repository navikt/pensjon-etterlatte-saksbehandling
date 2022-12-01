package no.nav.etterlatte.libs.ktor

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.testing.testApplication
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RestModuleTest {

    private val server = MockOAuth2Server()
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

    @BeforeAll
    fun beforeAll() {
        server.start()
        System.setProperty("AZURE_APP_WELL_KNOWN_URL", server.wellKnownUrl(ISSUER_ID).toString())
        System.setProperty("AZURE_APP_CLIENT_ID", CLIENT_ID)
    }

    @AfterAll
    fun afterAll() {
        server.shutdown()
    }

    @Test
    fun `skal sette opp to endepunkter med autentisering`() {
        testApplication {
            application {
                restModule {
                    route1()
                    route2()
                }
            }

            val response1 = client.get("/test") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            val response2 = client.get("/test2") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            assertEquals(OK, response1.status)
            assertEquals(OK, response2.status)
        }
    }

    @Test
    fun `skal kunne lese og skrive json payload`() {
        testApplication {
            application {
                restModule { route1() }
            }

            val testObjekt = TestObjektDto("test", 1)

            val response = client.post("/test") {
                setBody(testObjekt.toJson())
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            val testObjektSvar = objectMapper.readValue(response.bodyAsText(), TestObjektDto::class.java)

            assertEquals(OK, response.status)
            assertEquals("lest", testObjektSvar.verdi1)
        }
    }

    @Test
    fun `skal returnere internal server error og logge dersom noe feiler`() {
        testApplication {
            application { restModule { route1() } }

            val response = client.get("/test/fails") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            assertEquals(HttpStatusCode.InternalServerError, response.status)
        }
    }

    @Test
    fun `skal svare paa helsesjekk uten autentisering`() {
        testApplication {
            application { restModule { route1() } }

            val response1 = client.get("/health/isalive")
            assertEquals(OK, response1.status)

            val response2 = client.get("/health/isready")
            assertEquals(OK, response2.status)
        }
    }

    private fun Route.route1() {
        route("/test") {
            get("/") {
                call.respond(OK)
            }
            post("/") {
                val testObjekt = call.receive<TestObjektDto>()
                call.respond(OK, testObjekt.copy(verdi1 = "lest"))
            }
            get("/fails") {
                throw RuntimeException("Noe feilet")
            }
        }
    }

    private fun Route.route2() {
        route("/test2") {
            get("/") {
                call.respond(OK)
            }
        }
    }

    private data class TestObjektDto(val verdi1: String, val verdi2: Int)

    private companion object {
        const val CLIENT_ID = "azure-id for saksbehandler"
        const val ISSUER_ID = "azure"
    }
}