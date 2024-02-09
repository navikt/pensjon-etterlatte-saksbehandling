package no.nav.etterlatte.behandling

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import io.ktor.serialization.jackson.jackson
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.domain.SaksbehandlerEnhet
import no.nav.etterlatte.behandling.klienter.NavAnsattKlient
import no.nav.etterlatte.behandling.klienter.NavAnsattKlientImpl
import no.nav.etterlatte.behandling.klienter.Norg2Klient
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.libs.common.toJson
import org.junit.jupiter.api.Test

class BrukerServiceTest {
    private val defaultHeaders = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))

    private val saksbehandlerEnheter =
        listOf(
            SaksbehandlerEnhet("id1", "navn1"),
            SaksbehandlerEnhet("id2", "navn2"),
            SaksbehandlerEnhet("id3", "navn3"),
        )

    private val testNavIdent = "ident1"

    private fun klient(): NavAnsattKlient =
        NavAnsattKlientImpl(
            mockHttpClient(
                saksbehandlerEnheter,
                testNavIdent,
            ),
            "",
        )

    private val pdltjenesterKlient = mockk<PdlTjenesterKlient>()
    private val norg2Klient = mockk<Norg2Klient>()

    @Test
    fun `hent enheter skal returnere en liste av enheter`() {
        val service = klient()

        runBlocking {
            val resultat = service.hentEnheterForSaksbehandler(testNavIdent)

            resultat.size shouldBeExactly 3

            resultat shouldContainExactly saksbehandlerEnheter
        }
    }

    fun harTilgangTilEnhet(
        enheter: List<SaksbehandlerEnhet>,
        enhetId: String,
    ) = enheter.any { enhet -> enhet.id == enhetId }

    @Test
    fun `enhet tilgang skal returnere true naar det er tilgang`() {
        val service = klient()

        runBlocking {
            val resultat = harTilgangTilEnhet(service.hentEnheterForSaksbehandler(testNavIdent), "id1")

            resultat shouldBe true
        }
    }

    @Test
    fun `enhet tilgang skal returnere false naar det ikke er tilgang`() {
        val service = klient()

        runBlocking {
            val resultat = harTilgangTilEnhet(service.hentEnheterForSaksbehandler(testNavIdent), "id4")

            resultat shouldBe false
        }
    }

    private fun mockHttpClient(
        respons: Any,
        ident: String,
    ): HttpClient {
        val httpClient =
            HttpClient(MockEngine) {
                engine {
                    addHandler { request ->
                        when (request.url.fullPath) {
                            "/navansatt/$ident/enheter" ->
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
