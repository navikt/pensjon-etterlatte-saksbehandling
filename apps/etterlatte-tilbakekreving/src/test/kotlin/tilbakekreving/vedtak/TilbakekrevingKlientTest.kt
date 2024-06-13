package no.nav.etterlatte.tilbakekreving.vedtak

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import io.ktor.serialization.jackson.JacksonConverter
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.tilbakekreving.hendelse.TilbakekrevingHendelseRepository
import no.nav.etterlatte.tilbakekreving.hendelse.TilbakekrevingHendelseType
import no.nav.etterlatte.tilbakekreving.tilbakekrevingsvedtak
import no.nav.okonomi.tilbakekrevingservice.TilbakekrevingsvedtakResponse
import no.nav.tilbakekreving.typer.v1.MmelDto
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class TilbakekrevingKlientTest {
    private val hendelseRepository = mockk<TilbakekrevingHendelseRepository>(relaxed = true)

    @Test
    fun `skal kunne sende tilbakekrevingvedtak og haandtere ok fra tilbakekrevingskomponenten`() {
        val response =
            TilbakekrevingsvedtakResponse().apply {
                mmel = MmelDto().apply { alvorlighetsgrad = "00" }
            }

        val httpClient = mockedHttpClient("/tilbakekreving/tilbakekrevingsvedtak", HttpMethod.Post, response)
        val tilbakekrevingKlient = TilbakekrevingKlient("", httpClient, hendelseRepository)

        val tilbakekrevingsvedtak = tilbakekrevingsvedtak()
        tilbakekrevingKlient.sendTilbakekrevingsvedtak(tilbakekrevingsvedtak)

        verify {
            hendelseRepository.lagreTilbakekrevingHendelse(
                tilbakekrevingsvedtak.sakId,
                any(),
                TilbakekrevingHendelseType.TILBAKEKREVINGSVEDTAK_SENDT,
            )
            hendelseRepository.lagreTilbakekrevingHendelse(
                tilbakekrevingsvedtak.sakId,
                any(),
                TilbakekrevingHendelseType.TILBAKEKREVINGSVEDTAK_KVITTERING,
            )
        }
    }

    @Test
    fun `skal kunne sende tilbakekrevingvedtak og haandtere feilmelding fra tilbakekrevingskomponenten`() {
        val response =
            TilbakekrevingsvedtakResponse().apply {
                mmel = MmelDto().apply { alvorlighetsgrad = "08" }
            }

        val httpClient = mockedHttpClient("/tilbakekreving/tilbakekrevingsvedtak", HttpMethod.Post, response)
        val tilbakekrevingKlient = TilbakekrevingKlient("", httpClient, hendelseRepository)

        val tilbakekrevingsvedtak = tilbakekrevingsvedtak()

        assertThrows<Exception>("Tilbakekrevingsvedtak feilet med alvorlighetsgrad 08") {
            tilbakekrevingKlient.sendTilbakekrevingsvedtak(tilbakekrevingsvedtak)
        }

        verify {
            hendelseRepository.lagreTilbakekrevingHendelse(
                tilbakekrevingsvedtak.sakId,
                any(),
                TilbakekrevingHendelseType.TILBAKEKREVINGSVEDTAK_SENDT,
            )
            hendelseRepository.lagreTilbakekrevingHendelse(
                tilbakekrevingsvedtak.sakId,
                any(),
                TilbakekrevingHendelseType.TILBAKEKREVINGSVEDTAK_KVITTERING,
            )
        }
    }

    private fun mockedHttpClient(
        url: String,
        method: HttpMethod,
        response: Any,
    ): HttpClient =
        HttpClient(MockEngine) {
            expectSuccess = true
            install(ContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter(objectMapper))
            }

            engine {
                addHandler { request ->
                    when {
                        request.url.fullPath == url && request.method == method ->
                            respond(
                                response.toJson(),
                                HttpStatusCode.OK,
                                headersOf("Content-Type" to listOf(ContentType.Application.Json.toString())),
                            )

                        else -> error("Unhandled ${request.url.fullPath}")
                    }
                }
            }
        }
}
