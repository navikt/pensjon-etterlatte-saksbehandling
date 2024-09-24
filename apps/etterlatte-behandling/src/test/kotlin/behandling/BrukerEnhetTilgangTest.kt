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
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.klienter.AxsysKlient
import no.nav.etterlatte.behandling.klienter.AxsysKlientImpl
import no.nav.etterlatte.behandling.klienter.Enheter
import no.nav.etterlatte.behandling.klienter.EnhetslisteResponse
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.saksbehandler.SaksbehandlerEnhet
import org.junit.jupiter.api.Test

class BrukerEnhetTilgangTest {
    private val defaultHeaders = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))

    private val saksbehandlerEnheter =
        listOf(
            SaksbehandlerEnhet(no.nav.etterlatte.common.Enheter.STEINKJER.enhetNr, "navn1"),
            SaksbehandlerEnhet(no.nav.etterlatte.common.Enheter.PORSGRUNN.enhetNr, "navn2"),
            SaksbehandlerEnhet(no.nav.etterlatte.common.Enheter.AALESUND.enhetNr, "navn3"),
        )

    private val testNavIdent = "ident1"

    private fun klient(): AxsysKlient =
        AxsysKlientImpl(
            mockHttpClient(
                EnhetslisteResponse(saksbehandlerEnheter.map { Enheter(it.enhetsNummer, null, it.navn) }),
                testNavIdent,
            ),
            "",
        )

    @Test
    fun `hent enheter skal returnere en liste av enheter`() {
        val service = klient()

        runBlocking {
            val resultat = service.hentEnheterForIdent(testNavIdent)

            resultat.size shouldBeExactly 3

            resultat shouldContainExactly saksbehandlerEnheter
        }
    }

    fun harTilgangTilEnhet(
        enheter: List<SaksbehandlerEnhet>,
        enhetId: Enhetsnummer,
    ) = enheter.any { enhet -> enhet.enhetsNummer == enhetId }

    @Test
    fun `enhet tilgang skal returnere true naar det er tilgang`() {
        val service = klient()

        runBlocking {
            val resultat =
                harTilgangTilEnhet(service.hentEnheterForIdent(testNavIdent), no.nav.etterlatte.common.Enheter.defaultEnhet.enhetNr)

            resultat shouldBe true
        }
    }

    @Test
    fun `enhet tilgang skal returnere false naar det ikke er tilgang`() {
        val service = klient()

        runBlocking {
            val resultat = harTilgangTilEnhet(service.hentEnheterForIdent(testNavIdent), Enhetsnummer("9867"))

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
                            "/api/v2/tilgang/$ident?inkluderAlleEnheter=false" ->
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
