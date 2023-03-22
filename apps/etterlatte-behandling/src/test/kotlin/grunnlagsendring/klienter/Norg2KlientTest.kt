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
import no.nav.etterlatte.behandling.domain.ArbeidsFordelingEnhet
import no.nav.etterlatte.behandling.klienter.Norg2Klient
import no.nav.etterlatte.behandling.klienter.Norg2KlientImpl
import org.junit.jupiter.api.Test

internal class Norg2KlientTest {
    private val defaultHeaders = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))

    @Test
    fun `hent best skal returnere en liste av enheter`() {
        val forventet = """
            [
                {
                  "enhetId": 100000589,
                  "navn": "NAV Familie- og pensjonsytelser Steinkjer",
                  "enhetNr": "4817",
                  "antallRessurser": 0,
                  "status": "Aktiv",
                  "orgNivaa": "SPESEN",
                  "type": "FPY",
                  "organisasjonsnummer": null,
                  "underEtableringDato": "1970-01-01",
                  "aktiveringsdato": "1970-01-01",
                  "underAvviklingDato": null,
                  "nedleggelsesdato": null,
                  "oppgavebehandler": true,
                  "versjon": 2,
                  "sosialeTjenester": null,
                  "kanalstrategi": null,
                  "orgNrTilKommunaltNavKontor": null
                }
            ]
        """.trimIndent()

        val klient = mockHttpClient(forventet)

        val norg2Klient: Norg2Klient = Norg2KlientImpl(klient, "")

        val resultat = norg2Klient.hentEnheterForOmraade("EYB", "0301")

        resultat.size shouldBeExactly 1

        resultat shouldContainExactly listOf(ArbeidsFordelingEnhet("NAV Familie- og pensjonsytelser Steinkjer", "4817"))
    }

    private fun mockHttpClient(jsonRespons: String): HttpClient {
        val httpClient = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    when (request.url.fullPath) {
                        "/api/v1/arbeidsfordeling/enheter/bestmatch" -> respond(
                            jsonRespons,
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