package no.nav.etterlatte.libs.ktor

import com.fasterxml.jackson.databind.JsonMappingException
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.contentType
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.etterlatte.libs.common.BEHANDLINGSID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.BehandlingTilgangsSjekk
import no.nav.etterlatte.libs.common.FoedselsnummerDTO
import no.nav.etterlatte.libs.common.PersonTilgangsSjekk
import no.nav.etterlatte.libs.common.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.SakTilgangsSjekk
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.withBehandlingId
import no.nav.etterlatte.libs.common.withFoedselsnummer
import no.nav.etterlatte.libs.common.withSakId
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import testsupport.buildTestApplicationConfigurationForOauth
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RestModuleTest {
    private val behandlingTilgangsSjekkMock = mockk<BehandlingTilgangsSjekk>()
    private val sakTilgangsSjekkMock = mockk<SakTilgangsSjekk>()
    private val personTilgangsSjekkMock = mockk<PersonTilgangsSjekk>()

    private val server = MockOAuth2Server()
    private lateinit var hoconApplicationConfig: HoconApplicationConfig
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

    @BeforeAll
    fun beforeAll() {
        server.start()
        val httpServer = server.config.httpServer
        hoconApplicationConfig = buildTestApplicationConfigurationForOauth(httpServer.port(), AZURE_ISSUER, CLIENT_ID)
    }

    @AfterAll
    fun afterAll() {
        server.shutdown()
    }

    @Test
    fun `skal sette opp to endepunkter med autentisering`() {
        testApplication {
            environment {
                config = hoconApplicationConfig
            }
            application {
                restModule(this.log) {
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
    fun `skal kun svare ok dersom bruker har tilgang`() {
        coEvery { behandlingTilgangsSjekkMock.harTilgangTilBehandling(any(), any()) } returns true
        coEvery { sakTilgangsSjekkMock.harTilgangTilSak(any(), any()) } returns true
        coEvery { personTilgangsSjekkMock.harTilgangTilPerson(any(), any()) } returns true

        testApplication {
            environment {
                config = hoconApplicationConfig
            }
            application { restModule(this.log) { tilgangTestRoute() } }.also { setReady() }

            client.get("/behandling/${UUID.randomUUID()}") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }.let { assertEquals(OK, it.status) }
            client.get("/sak/1") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }.let { assertEquals(OK, it.status) }
            client.post("/person") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(FoedselsnummerDTO("30106519672").toJson())
            }.let { assertEquals(OK, it.status) }

            coEvery { behandlingTilgangsSjekkMock.harTilgangTilBehandling(any(), any()) } returns false
            coEvery { sakTilgangsSjekkMock.harTilgangTilSak(any(), any()) } returns false
            coEvery { personTilgangsSjekkMock.harTilgangTilPerson(any(), any()) } returns false

            client.get("/behandling/${UUID.randomUUID()}") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }.let { assertEquals(NotFound, it.status) }
            client.get("/sak/1") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }.let { assertEquals(NotFound, it.status) }
            client.post("/person") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(FoedselsnummerDTO("30106519672").toJson())
            }.let { assertEquals(NotFound, it.status) }
        }
    }

    @Test
    fun `skal kunne lese og skrive json payload`() {
        testApplication {
            environment {
                config = hoconApplicationConfig
            }
            application {
                restModule(this.log) { route1() }
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
            environment {
                config = hoconApplicationConfig
            }
            application { restModule(this.log) { route1() } }

            val response = client.get("/test/fails") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            assertEquals(HttpStatusCode.InternalServerError, response.status)
        }
    }

    @Test
    fun `skal svare paa helsesjekk uten autentisering`() {
        testApplication {
            environment {
                config = hoconApplicationConfig
            }
            application { restModule(this.log) { route1() } }.also { setReady() }

            val response1 = client.get("/health/isalive")
            assertEquals(OK, response1.status)

            val response2 = client.get("/health/isready")
            assertEquals(OK, response2.status)
        }
    }

    @Test
    fun `skjulAllePotensielleFnr fjerner alle forekomster av nøyaktig 11 tall på rad fra tekst`() {
        val potensieltTelefonnummer = "004722225555"
        val tekstMedTilfeldigeTall = "qwerty99991231231231299uiopasdf 2134 arstdhnei 111"
        val noeyaktigElleveTall = "12345678901"
        val potensieltLoggentry = "<- 200 GET /api/12111111111/person/12111111111/roller"

        assertEquals(potensieltTelefonnummer, skjulAllePotensielleFnr(potensieltTelefonnummer))
        assertEquals(tekstMedTilfeldigeTall, skjulAllePotensielleFnr(tekstMedTilfeldigeTall))
        assertEquals("***********", skjulAllePotensielleFnr(noeyaktigElleveTall))
        assertTrue(potensieltLoggentry.contains("12111111111"))
        assertFalse(skjulAllePotensielleFnr(potensieltLoggentry).contains("12111111111"))
    }

    @Test
    fun `Skal finne deserialiseringsexceptions i nestede exceptions`() {
        assertFalse(Exception("Hello").erDeserialiseringsException())
        assertFalse(Exception("Hello", OutOfMemoryError()).erDeserialiseringsException())

        val jacksonException = JsonMappingException("Error")
        val wrappedException = java.lang.RuntimeException("Err", jacksonException)

        assertTrue(jacksonException.erDeserialiseringsException())
        assertTrue(wrappedException.erDeserialiseringsException())
    }

    @Test
    fun `metrics test`() {
        testApplication {
            environment {
                config = hoconApplicationConfig
            }
            application {
                restModule(this.log, withMetrics = true) { route1() }
            }

            client.get("/metrics").also {
                val body: String = it.body()

                assertEquals(OK, it.status)
                assertTrue(body.contains("HELP"))
            }
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

    private fun Route.tilgangTestRoute() {
        route("") {
            get("/behandling/{$BEHANDLINGSID_CALL_PARAMETER}") {
                withBehandlingId(behandlingTilgangsSjekkMock) {
                    call.respond(OK)
                }
            }
            get("sak/{$SAKID_CALL_PARAMETER}") {
                withSakId(sakTilgangsSjekkMock) {
                    call.respond(OK)
                }
            }
            post("person") {
                withFoedselsnummer(personTilgangsSjekkMock) {
                    call.respond(OK)
                }
            }
        }
    }

    private data class TestObjektDto(val verdi1: String, val verdi2: Int)

    private companion object {
        const val CLIENT_ID = "azure-id for saksbehandler"
    }
}