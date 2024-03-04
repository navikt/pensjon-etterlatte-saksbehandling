package no.nav.etterlatte.grunnlagsendring.klienter

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import io.kotest.matchers.ints.shouldBeExactly
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import io.ktor.serialization.jackson.jackson
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.domain.SaksbehandlerTema
import no.nav.etterlatte.behandling.klienter.NavAnsattKlient
import no.nav.etterlatte.behandling.klienter.NavAnsattKlientImpl
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.ktor.ServiceStatus
import org.junit.jupiter.api.Test

internal class NavAnsattKlientTest {
    private val defaultHeaders = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))

    @Test
    fun `ping it OK`() {
        val klient =
            HttpClient(MockEngine) {
                engine {
                    addHandler { request ->
                        when (request.url.fullPath) {
                            "/navansatt/ping" ->
                                respond(
                                    "OK",
                                    HttpStatusCode.OK,
                                    defaultHeaders,
                                )

                            else -> error("Unhandled ${request.url.fullPath}")
                        }
                    }
                }
                expectSuccess = true
            }

        val navAnsattKlient = NavAnsattKlientImpl(klient, "navansatt")
        runBlocking {
            val ping = navAnsattKlient.ping()
            ping.status shouldBeEqualComparingTo ServiceStatus.UP
        }
    }

    @Test
    fun `ping it 404`() {
        val klient =
            HttpClient(MockEngine) {
                engine {
                    addHandler { request ->
                        when (request.url.fullPath) {
                            "/navansatt/ping" -> respondError(HttpStatusCode.NotFound)
                            else -> error("Unhandled ${request.url.fullPath}")
                        }
                    }
                }
                expectSuccess = true
            }

        val navAnsattKlient = NavAnsattKlientImpl(klient, "navansatt")
        runBlocking {
            val ping = navAnsattKlient.ping()
            ping.status shouldBeEqualComparingTo ServiceStatus.DOWN
        }
    }

    @Test
    fun `hent fagomraader skal returnere en liste av tema`() {
        val testNavIdent = "ident1"

        val fagomraader =
            listOf(
                SaksbehandlerTema("tema1"),
                SaksbehandlerTema("tema2"),
            )

        val klient =
            mockHttpClient(
                fagomraader,
                testNavIdent,
                "fagomrader",
            )

        val navAnsattKlient: NavAnsattKlient = NavAnsattKlientImpl(klient, "")

        runBlocking {
            val resultat = navAnsattKlient.hentTemaForSaksbehandler(testNavIdent) // TODO: remove all off this

            resultat.size shouldBeExactly 2

            resultat shouldContainExactly fagomraader
        }
    }

    private fun mockHttpClient(
        respons: Any,
        ident: String,
        suffix: String,
    ): HttpClient {
        val httpClient =
            HttpClient(MockEngine) {
                engine {
                    addHandler { request ->
                        when (request.url.fullPath) {
                            "/navansatt/$ident/$suffix" ->
                                respond(
                                    respons.toJson(),
                                    HttpStatusCode.OK,
                                    defaultHeaders,
                                )

                            else -> error("Unhandled ${request.url.fullPath}")
                        }
                    }
                }
                expectSuccess = true
                install(ContentNegotiation) {
                    jackson {
                        registerModule(JavaTimeModule())
                    }
                }
            }

        return httpClient
    }
}
