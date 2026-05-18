package no.nav.etterlatte.tilbakekreving.klienter

import io.kotest.matchers.shouldBe
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
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingskomponentenFeil
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.tilbakekreving.TilbakekrevingHendelse
import no.nav.etterlatte.tilbakekreving.TilbakekrevingHendelseRepository
import no.nav.etterlatte.tilbakekreving.TilbakekrevingHendelseStatus
import no.nav.etterlatte.tilbakekreving.TilbakekrevingHendelseType
import no.nav.etterlatte.tilbakekreving.kravgrunnlag.KravgrunnlagJaxb
import no.nav.etterlatte.tilbakekreving.readFile
import no.nav.etterlatte.tilbakekreving.tilbakekrevingsvedtak
import no.nav.okonomi.tilbakekrevingservice.KravgrunnlagHentDetaljResponse
import no.nav.okonomi.tilbakekrevingservice.TilbakekrevingsvedtakRequest
import no.nav.okonomi.tilbakekrevingservice.TilbakekrevingsvedtakResponse
import no.nav.tilbakekreving.tilbakekrevingsvedtak.vedtak.v1.TilbakekrevingsvedtakDto
import no.nav.tilbakekreving.typer.v1.MmelDto
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.math.BigInteger
import java.time.Month
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.UUID

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
    fun `skal feile hvis tilbakekrevingskomponenten sier behandlet før og vi ikke har en gyldig kvittering`() {
        val alleredeBehandletResponse =
            TilbakekrevingsvedtakResponse().apply {
                mmel =
                    MmelDto().apply {
                        alvorlighetsgrad = "08"
                        beskrMelding = "Vedtaket er behandlet(BEHA): noe annen info her"
                    }
            }
        val vedtakId = 123L
        val sakId = SakId(27L)

        val httpClient = mockedHttpClient("/tilbakekreving/tilbakekrevingsvedtak", HttpMethod.Post, alleredeBehandletResponse)
        val tilbakekrevingskomponentenKlient = TilbakekrevingskomponentenKlient("", httpClient, hendelseRepository)
        val tilbakekrevingsvedtak = tilbakekrevingsvedtak(vedtakId = vedtakId, sakId = sakId)

        every { hendelseRepository.finnHendelserForSak(sakId) }.returns(emptyList())

        assertThrows<TilbakekrevingskomponentenFeil> {
            tilbakekrevingskomponentenKlient.sendTilbakekrevingsvedtak(tilbakekrevingsvedtak)
        }
    }

    @Test
    fun `skal ikke feile hvis tilbakekrevingskomponenten sier behandlet før og vi har en gyldig kvittering for samme vedtak`() {
        val alleredeBehandletResponse =
            TilbakekrevingsvedtakResponse().apply {
                mmel =
                    MmelDto().apply {
                        alvorlighetsgrad = "08"
                        beskrMelding = "Vedtaket er behandlet(BEHA): noe annen info her"
                    }
            }
        val behandletOkResponse =
            TilbakekrevingsvedtakResponse().apply {
                mmel =
                    MmelDto().apply {
                        alvorlighetsgrad = "00"
                        beskrMelding = "Vedtaket er oppdatert"
                    }
            }

        val vedtakId = 123L
        val sakId = SakId(27L)

        val tilbakekrevingViLagrer =
            TilbakekrevingsvedtakRequest().apply {
                this.tilbakekrevingsvedtak =
                    TilbakekrevingsvedtakDto().apply {
                        this.vedtakId = BigInteger.valueOf(vedtakId)
                    }
            }
        val httpClient = mockedHttpClient("/tilbakekreving/tilbakekrevingsvedtak", HttpMethod.Post, alleredeBehandletResponse)
        val tilbakekrevingskomponentenKlient = TilbakekrevingskomponentenKlient("", httpClient, hendelseRepository)
        val tilbakekrevingsvedtak = tilbakekrevingsvedtak(vedtakId = vedtakId, sakId = sakId)

        every { hendelseRepository.finnHendelserForSak(sakId) }.returns(
            listOf(
                tilbakekrevingHendelse(
                    sakId = sakId,
                    type = TilbakekrevingHendelseType.TILBAKEKREVINGSVEDTAK_SENDT,
                    payload = tilbakekrevingViLagrer.toJson(),
                    opprettet = Tidspunkt.now().minus(2, ChronoUnit.HOURS),
                ),
                tilbakekrevingHendelse(
                    sakId = sakId,
                    type = TilbakekrevingHendelseType.TILBAKEKREVINGSVEDTAK_KVITTERING,
                    payload = behandletOkResponse.toJson(),
                    opprettet = Tidspunkt.now().minus(2, ChronoUnit.HOURS).plus(1, ChronoUnit.MINUTES),
                ),
                tilbakekrevingHendelse(
                    sakId = sakId,
                    type = TilbakekrevingHendelseType.TILBAKEKREVINGSVEDTAK_SENDT,
                    payload = tilbakekrevingViLagrer.toJson(),
                    opprettet = Tidspunkt.now().minus(1, ChronoUnit.HOURS),
                ),
                tilbakekrevingHendelse(
                    sakId = sakId,
                    type = TilbakekrevingHendelseType.TILBAKEKREVINGSVEDTAK_KVITTERING,
                    payload = alleredeBehandletResponse.toJson(),
                    opprettet = Tidspunkt.now().minus(1, ChronoUnit.HOURS).plus(1, ChronoUnit.MINUTES),
                ),
                // Denne vil komme fra kallet vi gjør nå, vi bare mocker det i responsen
                tilbakekrevingHendelse(
                    sakId = sakId,
                    type = TilbakekrevingHendelseType.TILBAKEKREVINGSVEDTAK_SENDT,
                    payload = tilbakekrevingViLagrer.toJson(),
                    opprettet = Tidspunkt.now(),
                ),
            ),
        )

        assertDoesNotThrow {
            tilbakekrevingskomponentenKlient.sendTilbakekrevingsvedtak(tilbakekrevingsvedtak)
        }
    }

    @Test
    fun `skal feile hvis tilbakekrevingskomponenten sier behandlet før og vi har en gyldig kvittering for et annet vedtak kun`() {
        val alleredeBehandletResponse =
            TilbakekrevingsvedtakResponse().apply {
                mmel =
                    MmelDto().apply {
                        alvorlighetsgrad = "08"
                        beskrMelding = "Vedtaket er behandlet(BEHA): noe annen info her"
                    }
            }
        val behandletOkResponse =
            TilbakekrevingsvedtakResponse().apply {
                mmel =
                    MmelDto().apply {
                        alvorlighetsgrad = "00"
                        beskrMelding = "Vedtaket er oppdatert"
                    }
            }

        val forrigeVedtakId = 123L
        val nyVedtakId = 456L

        val sakId = SakId(27L)

        val forrigeTilbakekrevingViLagret =
            TilbakekrevingsvedtakRequest().apply {
                this.tilbakekrevingsvedtak =
                    TilbakekrevingsvedtakDto().apply {
                        this.vedtakId = BigInteger.valueOf(forrigeVedtakId)
                    }
            }
        val nyTilbakekrevingViLagrer =
            TilbakekrevingsvedtakRequest().apply {
                this.tilbakekrevingsvedtak =
                    TilbakekrevingsvedtakDto().apply {
                        this.vedtakId = BigInteger.valueOf(nyVedtakId)
                    }
            }

        val httpClient = mockedHttpClient("/tilbakekreving/tilbakekrevingsvedtak", HttpMethod.Post, alleredeBehandletResponse)
        val tilbakekrevingskomponentenKlient = TilbakekrevingskomponentenKlient("", httpClient, hendelseRepository)
        val tilbakekrevingsvedtak = tilbakekrevingsvedtak(vedtakId = nyVedtakId, sakId = sakId)

        every { hendelseRepository.finnHendelserForSak(sakId) }.returns(
            listOf(
                tilbakekrevingHendelse(
                    sakId = sakId,
                    type = TilbakekrevingHendelseType.TILBAKEKREVINGSVEDTAK_SENDT,
                    payload = forrigeTilbakekrevingViLagret.toJson(),
                    opprettet = Tidspunkt.now().minus(2, ChronoUnit.HOURS),
                ),
                tilbakekrevingHendelse(
                    sakId = sakId,
                    type = TilbakekrevingHendelseType.TILBAKEKREVINGSVEDTAK_KVITTERING,
                    payload = behandletOkResponse.toJson(),
                    opprettet = Tidspunkt.now().minus(2, ChronoUnit.HOURS).plus(1, ChronoUnit.MINUTES),
                ),
                tilbakekrevingHendelse(
                    sakId = sakId,
                    type = TilbakekrevingHendelseType.TILBAKEKREVINGSVEDTAK_SENDT,
                    payload = nyTilbakekrevingViLagrer.toJson(),
                    opprettet = Tidspunkt.now().minus(1, ChronoUnit.HOURS),
                ),
                tilbakekrevingHendelse(
                    sakId = sakId,
                    type = TilbakekrevingHendelseType.TILBAKEKREVINGSVEDTAK_KVITTERING,
                    payload = alleredeBehandletResponse.toJson(),
                    opprettet = Tidspunkt.now().minus(1, ChronoUnit.HOURS).plus(1, ChronoUnit.MINUTES),
                ),
                // Denne vil komme fra kallet vi gjør nå, vi bare mocker det i responsen
                tilbakekrevingHendelse(
                    sakId = sakId,
                    type = TilbakekrevingHendelseType.TILBAKEKREVINGSVEDTAK_SENDT,
                    payload = nyTilbakekrevingViLagrer.toJson(),
                    opprettet = Tidspunkt.now(),
                ),
            ),
        )

        assertThrows<TilbakekrevingskomponentenFeil> {
            tilbakekrevingskomponentenKlient.sendTilbakekrevingsvedtak(tilbakekrevingsvedtak)
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

        val kravgrunnlagResponse =
            krevIkkeNull(tilbakekrevingskomponentenKlient.hentKravgrunnlag(sakId, 123L)) {
                "Vi har satt opp en respons med kravgrunnlag"
            }

        kravgrunnlagResponse.perioder.first().periode.let {
            it.fraOgMed shouldBe YearMonth.of(2022, Month.APRIL)
            it.tilOgMed shouldBe YearMonth.of(2022, Month.APRIL)
        }

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
                        request.url.fullPath == url && request.method == method -> {
                            respond(
                                response.toJson(),
                                HttpStatusCode.OK,
                                headersOf("Content-Type" to listOf(ContentType.Application.Json.toString())),
                            )
                        }

                        else -> {
                            error("Unhandled ${request.url.fullPath}")
                        }
                    }
                }
            }
        }

    private fun tilbakekrevingHendelse(
        sakId: SakId,
        type: TilbakekrevingHendelseType,
        payload: String,
        opprettet: Tidspunkt = Tidspunkt.now(),
        status: TilbakekrevingHendelseStatus = TilbakekrevingHendelseStatus.FERDIGSTILT,
        jmsTimeStamp: Tidspunkt? = null,
    ): TilbakekrevingHendelse =
        TilbakekrevingHendelse(
            id = UUID.randomUUID(),
            opprettet = opprettet,
            sakId = sakId,
            payload = payload,
            status = status,
            type = type,
            jmsTimestamp = jmsTimeStamp,
        )
}
