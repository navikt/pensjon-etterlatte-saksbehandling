package no.nav.etterlatte.grunnlagsendring.klienter

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.ints.shouldBeExactly
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import io.ktor.serialization.jackson.jackson
import no.nav.etterlatte.behandling.domain.SaksbehandlerEnhet
import no.nav.etterlatte.behandling.domain.SaksbehandlerTema
import no.nav.etterlatte.behandling.klienter.NavAnsattKlient
import no.nav.etterlatte.behandling.klienter.NavAnsattKlientImpl
import no.nav.etterlatte.libs.common.toJson
import org.junit.jupiter.api.Test

internal class NavAnsattKlientTest {

    private val defaultHeaders = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))

    @Test
    fun `hent enheter skal returnere en liste av enheter`() {
        val testNavIdent = "ident1"

        val saksbehandlerEnheter = listOf(
            SaksbehandlerEnhet("id1", "navn1"),
            SaksbehandlerEnhet("id2", "navn2"),
            SaksbehandlerEnhet("id3", "navn3")
        )

        val klient = mockHttpClient(
            saksbehandlerEnheter,
            testNavIdent,
            "enheter"
        )

        val navAnsattKlient: NavAnsattKlient = NavAnsattKlientImpl(klient, "")

        val resultat = navAnsattKlient.hentSaksbehandlerEnhet(testNavIdent)

        resultat.size shouldBeExactly 3

        resultat shouldContainExactly saksbehandlerEnheter
    }

    @Test
    fun `hent fagomraader skal returnere en liste av tema`() {
        val testNavIdent = "ident1"

        val fagomraader = listOf(
            SaksbehandlerTema("tema1"),
            SaksbehandlerTema("tema2")
        )

        val klient = mockHttpClient(
            fagomraader,
            testNavIdent,
            "fagomrader"
        )

        val navAnsattKlient: NavAnsattKlient = NavAnsattKlientImpl(klient, "")

        val resultat = navAnsattKlient.hentSaksbehandlerTema(testNavIdent)

        resultat.size shouldBeExactly 2

        resultat shouldContainExactly fagomraader
    }

    private fun mockHttpClient(respons: Any, ident: String, suffix: String): HttpClient {
        val httpClient = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    when (request.url.fullPath) {
                        "/navansatt/$ident/$suffix" -> respond(
                            respons.toJson(),
                            HttpStatusCode.OK,
                            defaultHeaders
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