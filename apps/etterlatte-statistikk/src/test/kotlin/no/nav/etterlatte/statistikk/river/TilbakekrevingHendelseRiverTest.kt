package no.nav.etterlatte.statistikk.river

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.UUID30
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.rapidsandrivers.CORRELATION_ID_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.TEKNISK_TID_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.lagParMedEventNameKey
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tilbakekreving.Grunnlagsbeloep
import no.nav.etterlatte.libs.common.tilbakekreving.KlasseKode
import no.nav.etterlatte.libs.common.tilbakekreving.KlasseType
import no.nav.etterlatte.libs.common.tilbakekreving.Kontrollfelt
import no.nav.etterlatte.libs.common.tilbakekreving.Kravgrunnlag
import no.nav.etterlatte.libs.common.tilbakekreving.KravgrunnlagId
import no.nav.etterlatte.libs.common.tilbakekreving.KravgrunnlagPeriode
import no.nav.etterlatte.libs.common.tilbakekreving.KravgrunnlagStatus
import no.nav.etterlatte.libs.common.tilbakekreving.NavIdent
import no.nav.etterlatte.libs.common.tilbakekreving.Periode
import no.nav.etterlatte.libs.common.tilbakekreving.SakId
import no.nav.etterlatte.libs.common.tilbakekreving.StatistikkTilbakekrevingDto
import no.nav.etterlatte.libs.common.tilbakekreving.TILBAKEKREVING_STATISTIKK_RIVER_KEY
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingBehandling
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingHendelseType
import no.nav.etterlatte.libs.common.tilbakekreving.VedtakId
import no.nav.etterlatte.statistikk.service.StatistikkService
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TilbakekrevingHendelseRiverTest {
    private val soeknadStatistikkService: StatistikkService =
        mockk {
            every { registrerStatistikkFortilbakkrevinghendelse(any(), any(), any()) } returns null
        }

    private val testRapid: TestRapid =
        TestRapid().apply {
            TilbakekrevinghendelseRiver(this, soeknadStatistikkService)
        }

    private fun tilbakekreving(sak: Sak) =
        TilbakekrevingBehandling.ny(
            sak = sak,
            kravgrunnlag =
                Kravgrunnlag(
                    kravgrunnlagId = KravgrunnlagId(123L),
                    sakId = SakId(474L),
                    vedtakId = VedtakId(2L),
                    kontrollFelt = Kontrollfelt(""),
                    status = KravgrunnlagStatus.ANNU,
                    saksbehandler = NavIdent(""),
                    referanse = UUID30(""),
                    perioder =
                        listOf(
                            KravgrunnlagPeriode(
                                periode =
                                    Periode(
                                        fraOgMed = YearMonth.of(2023, 1),
                                        tilOgMed = YearMonth.of(2023, 2),
                                    ),
                                skatt = BigDecimal(200),
                                grunnlagsbeloep =
                                    listOf(
                                        Grunnlagsbeloep(
                                            klasseKode = KlasseKode(""),
                                            klasseType = KlasseType.YTEL,
                                            bruttoUtbetaling = BigDecimal(1000),
                                            nyBruttoUtbetaling = BigDecimal(1200),
                                            bruttoTilbakekreving = BigDecimal(200),
                                            beloepSkalIkkeTilbakekreves = BigDecimal(200),
                                            skatteProsent = BigDecimal(20),
                                            resultat = null,
                                            skyld = null,
                                            aarsak = null,
                                        ),
                                        Grunnlagsbeloep(
                                            klasseKode = KlasseKode(""),
                                            klasseType = KlasseType.FEIL,
                                            bruttoUtbetaling = BigDecimal(0),
                                            nyBruttoUtbetaling = BigDecimal(0),
                                            bruttoTilbakekreving = BigDecimal(0),
                                            beloepSkalIkkeTilbakekreves = BigDecimal(0),
                                            skatteProsent = BigDecimal(0),
                                            resultat = null,
                                            skyld = null,
                                            aarsak = null,
                                        ),
                                    ),
                            ),
                        ),
                ),
            omgjoeringAvId = null,
        )

    @Test
    fun `skal ta i mot tilbakekrevinghendelse`() {
        val message =
            JsonMessage
                .newMessage(
                    mapOf(
                        TilbakekrevingHendelseType.OPPRETTET.lagParMedEventNameKey(),
                        CORRELATION_ID_KEY to UUID.randomUUID(),
                        TEKNISK_TID_KEY to LocalDateTime.now(),
                        TILBAKEKREVING_STATISTIKK_RIVER_KEY to
                            StatistikkTilbakekrevingDto(
                                UUID.randomUUID(),
                                tilbakekreving(Sak("ident", SakType.BARNEPENSJON, sakId1, Enheter.defaultEnhet.enhetNr, null, null)),
                                Tidspunkt.now(),
                                null,
                            ),
                    ),
                ).toJson()
        val inspector = testRapid.apply { sendTestMessage(message) }.inspektør
        Assertions.assertEquals(0, inspector.size) // kjører ingen STATISTIKK:REGISTRERT melding da null i mock

        verify { soeknadStatistikkService.registrerStatistikkFortilbakkrevinghendelse(any(), any(), any()) }
    }
}
