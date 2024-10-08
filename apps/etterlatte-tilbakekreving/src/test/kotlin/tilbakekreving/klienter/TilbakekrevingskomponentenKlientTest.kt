package no.nav.etterlatte.tilbakekreving.klienter

import io.kotest.matchers.shouldNotBe
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
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.tilbakekreving.TilbakekrevingHendelseRepository
import no.nav.etterlatte.tilbakekreving.TilbakekrevingHendelseType
import no.nav.etterlatte.tilbakekreving.kravgrunnlag.KravgrunnlagJaxb
import no.nav.etterlatte.tilbakekreving.readFile
import no.nav.etterlatte.tilbakekreving.tilbakekrevingsvedtak
import no.nav.okonomi.tilbakekrevingservice.KravgrunnlagHentDetaljResponse
import no.nav.okonomi.tilbakekrevingservice.TilbakekrevingsvedtakResponse
import no.nav.tilbakekreving.typer.v1.MmelDto
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class TilbakekrevingskomponentenKlientTest {
    private val hendelseRepository = mockk<TilbakekrevingHendelseRepository>(relaxed = true)

    @Test
    fun `skal kunne sende tilbakekrevingvedtak og haandtere ok fra tilbakekrevingskomponenten`() {
        val response =
            TilbakekrevingsvedtakResponse().apply {
                mmel = MmelDto().apply { alvorlighetsgrad = "00" }
            }

        val httpClient = mockedHttpClient("/tilbakekreving/tilbakekrevingsvedtak", HttpMethod.Post, response)
        val tilbakekrevingKlient = TilbakekrevingskomponentenKlient("", httpClient, hendelseRepository)

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
        val tilbakekrevingskomponentenKlient = TilbakekrevingskomponentenKlient("", httpClient, hendelseRepository)

        val tilbakekrevingsvedtak = tilbakekrevingsvedtak()

        assertThrows<Exception>("Tilbakekrevingsvedtak feilet med alvorlighetsgrad 08") {
            tilbakekrevingskomponentenKlient.sendTilbakekrevingsvedtak(tilbakekrevingsvedtak)
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

    @Test
    fun `skal kunne hente kravgrunnlag og haandtere ok fra tilbakekrevingskomponenten`() {
        val xml = readFile("/kravgrunnlag.xml")
        val kravgrunnlag = KravgrunnlagJaxb.toDetaljertKravgrunnlagDto(xml)

        val response =
            KravgrunnlagHentDetaljResponse().apply {
                mmel = MmelDto().apply { alvorlighetsgrad = "00" }
                detaljertkravgrunnlag = kravgrunnlag
            }

        val httpClient = mockedHttpClient("/tilbakekreving/kravgrunnlag", HttpMethod.Post, response)
        val tilbakekrevingskomponentenKlient = TilbakekrevingskomponentenKlient("", httpClient, hendelseRepository)
        val sakId = sakId1

        val kravgrunnlagResponse = tilbakekrevingskomponentenKlient.hentKravgrunnlag(sakId, 123L)

        kravgrunnlagResponse shouldNotBe null

        verify {
            hendelseRepository.lagreTilbakekrevingHendelse(
                sakId,
                any(),
                TilbakekrevingHendelseType.KRAVGRUNNLAG_FORESPOERSEL_SENDT,
            )
            hendelseRepository.lagreTilbakekrevingHendelse(
                sakId,
                any(),
                TilbakekrevingHendelseType.KRAVGRUNNLAG_FORESPOERSEL_KVITTERING,
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
