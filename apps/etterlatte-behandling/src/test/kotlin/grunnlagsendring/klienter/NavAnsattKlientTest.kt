package no.nav.etterlatte.grunnlagsendring.klienter

import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.klienter.NavAnsattKlientImpl
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
            val ping = navAnsattKlient.ping(konsument = "etterlatte-behandling")
            ping.result shouldBeEqualComparingTo ServiceStatus.UP
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
            val ping = navAnsattKlient.ping(konsument = "etterlatte-behandling")
            ping.result shouldBeEqualComparingTo ServiceStatus.DOWN
        }
    }
}
