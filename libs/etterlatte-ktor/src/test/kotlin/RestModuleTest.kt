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
import io.ktor.http.HttpStatusCode.Companion.InternalServerError
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.contentType
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.etterlatte.ktor.runServer
import no.nav.etterlatte.ktor.startRandomPort
import no.nav.etterlatte.ktor.token.issueSaksbehandlerToken
import no.nav.etterlatte.libs.common.feilhaandtering.ExceptionResponse
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.ktor.initialisering.setReady
import no.nav.etterlatte.libs.ktor.route.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.BehandlingTilgangsSjekk
import no.nav.etterlatte.libs.ktor.route.FoedselsnummerDTO
import no.nav.etterlatte.libs.ktor.route.PersonTilgangsSjekk
import no.nav.etterlatte.libs.ktor.route.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.SakTilgangsSjekk
import no.nav.etterlatte.libs.ktor.route.withBehandlingId
import no.nav.etterlatte.libs.ktor.route.withFoedselsnummer
import no.nav.etterlatte.libs.ktor.route.withSakId
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RestModuleTest {
    private val behandlingTilgangsSjekkMock = mockk<BehandlingTilgangsSjekk>()
    private val sakTilgangsSjekkMock = mockk<SakTilgangsSjekk>()
    private val personTilgangsSjekkMock = mockk<PersonTilgangsSjekk>()

    private val mockOAuth2Server = MockOAuth2Server()
    private val token: String by lazy { mockOAuth2Server.issueSaksbehandlerToken() }

    @BeforeAll
    fun beforeAll() {
        mockOAuth2Server.startRandomPort()
    }

    @AfterAll
    fun afterAll() {
        mockOAuth2Server.shutdown()
    }

    @Test
    fun `skal sette opp to endepunkter med autentisering`() {
        testApplication {
            runServer(mockOAuth2Server) {
                route1()
                route2()
            }

            val response1 =
                client.get("/test") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            val response2 =
                client.get("/test2") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            assertEquals(OK, response1.status)
            assertEquals(OK, response2.status)
        }
    }

    @Test
    fun `skal kun svare ok dersom bruker har tilgang`() {
        coEvery { behandlingTilgangsSjekkMock.harTilgangTilBehandling(any(), any(), any()) } returns true
        coEvery { sakTilgangsSjekkMock.harTilgangTilSak(any(), any(), any()) } returns true
        coEvery { personTilgangsSjekkMock.harTilgangTilPerson(any(), any(), any()) } returns true

        testApplication {
            runServer(mockOAuth2Server) {
                tilgangTestRoute()
            }.also { setReady() }

            client
                .get("/behandling/${UUID.randomUUID()}") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }.let { assertEquals(OK, it.status) }
            client
                .get("/sak/1") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }.let { assertEquals(OK, it.status) }
            client
                .post("/person") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    contentType(ContentType.Application.Json)
                    setBody(FoedselsnummerDTO("27458328671").toJson())
                }.let { assertEquals(OK, it.status) }

            coEvery { behandlingTilgangsSjekkMock.harTilgangTilBehandling(any(), any(), any()) } returns false
            coEvery { sakTilgangsSjekkMock.harTilgangTilSak(any(), any(), any()) } returns false
            coEvery { personTilgangsSjekkMock.harTilgangTilPerson(any(), any(), any()) } returns false

            client
                .get("/behandling/${UUID.randomUUID()}") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }.let { assertEquals(NotFound, it.status) }
            client
                .get("/sak/1") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }.let { assertEquals(NotFound, it.status) }
            client
                .post("/person") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    contentType(ContentType.Application.Json)
                    setBody(FoedselsnummerDTO("27458328671").toJson())
                }.let { assertEquals(NotFound, it.status) }
        }
    }

    @Test
    fun `skal kunne lese og skrive json payload`() {
        testApplication {
            runServer(mockOAuth2Server) {
                route1()
            }

            val testObjekt = TestObjektDto("test", 1)

            val response =
                client.post("/test") {
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
            runServer(mockOAuth2Server) {
                route1()
            }

            val response =
                client.get("/test/fails") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            assertEquals(InternalServerError, response.status)
        }
    }

    @Test
    fun `skal svare paa helsesjekk uten autentisering`() {
        testApplication {
            runServer(mockOAuth2Server) {
                route1()
            }.also { setReady() }

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

        val jacksonException = JsonMappingException.from(objectMapper.deserializationContext, "Error")
        val wrappedException = java.lang.RuntimeException("Err", jacksonException)

        assertTrue(jacksonException.erDeserialiseringsException())
        assertTrue(wrappedException.erDeserialiseringsException())
    }

    @Test
    fun `metrics test`() {
        testApplication {
            runServer(mockOAuth2Server, withMetrics = true) {
                route1()
            }

            client.get("/metrics").also {
                val body: String = it.body()

                assertEquals(OK, it.status)
                assertTrue(body.contains("HELP"))
            }
        }
    }

    @Test
    fun `statuspages håndterer kjente og ukjente exceptions`() {
        testApplication {
            val client =
                runServer(mockOAuth2Server) {
                    routesMedForskjelligeFeil()
                }

            client
                .get("ikke_funnet/exception") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    header(HttpHeaders.Accept, ContentType.Application.Json)
                }.also {
                    val body = it.body<ExceptionResponse>()
                    assertEquals("IKKE_FUNNET", body.code)
                    assertEquals(NotFound.value, body.status)
                }

            client
                .get("ikke_funnet/status") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    header(HttpHeaders.Accept, ContentType.Application.Json)
                }.also {
                    val body = it.body<ExceptionResponse>()
                    assertEquals(NotFound.value, body.status)
                    assertEquals("ROUTE_NOT_CONFIGURED", body.code)
                }
            client
                .get("route_som_ikke_finnes_og_vi/bare/fallbacker/til/ktorhaandtering") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    header(HttpHeaders.Accept, ContentType.Application.Json)
                }.also {
                    val body = it.body<ExceptionResponse>()
                    assertEquals(NotFound.value, body.status)
                    assertEquals("ROUTE_NOT_CONFIGURED", body.code)
                }

            client
                .get("intern/vilkaarlig") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    header(HttpHeaders.Accept, ContentType.Application.Json)
                }.also {
                    val bodyMapped = it.body<ExceptionResponse>()
                    assertEquals(InternalServerError.value, bodyMapped.status)
                }

            client
                .get("intern/exception") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    header(HttpHeaders.Accept, ContentType.Application.Json)
                }.also {
                    val bodyMapped = it.body<ExceptionResponse>()
                    assertEquals(InternalServerError.value, bodyMapped.status)
                }

            client
                .get("intern/status") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    header(HttpHeaders.Accept, ContentType.Application.Json)
                }.also {
                    val bodyMapped = it.body<ExceptionResponse>()
                    assertEquals(InternalServerError.value, bodyMapped.status)
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
            get("/behandling/{$BEHANDLINGID_CALL_PARAMETER}") {
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

    private fun Route.routesMedForskjelligeFeil() {
        route("ikke_funnet") {
            get("exception") {
                throw IkkeFunnetException("IKKE_FUNNET", "Vi fant ikke det du lette etter")
            }
            get("status") {
                call.respond(NotFound)
            }
        }

        route("intern") {
            get("vilkaarlig") {
                throw Exception("Whoops")
            }
            get("exception") {
                throw InternfeilException("Noe gikk galt :(")
            }
            get("status") {
                call.respond(InternalServerError)
            }
        }
    }

    private data class TestObjektDto(
        val verdi1: String,
        val verdi2: Int,
    )
}
