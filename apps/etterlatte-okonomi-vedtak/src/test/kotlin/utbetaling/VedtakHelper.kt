package no.nav.etterlatte.utbetaling

import no.nav.etterlatte.domene.vedtak.Attestasjon
import no.nav.etterlatte.domene.vedtak.Behandling
import no.nav.etterlatte.domene.vedtak.BehandlingType
import no.nav.etterlatte.domene.vedtak.Periode
import no.nav.etterlatte.domene.vedtak.Sak
import no.nav.etterlatte.domene.vedtak.UtbetalingsperiodeType
import no.nav.etterlatte.domene.vedtak.Vedtak
import no.nav.etterlatte.domene.vedtak.VedtakFattet
import no.nav.etterlatte.domene.vedtak.VedtakType
import no.nav.etterlatte.libs.common.toJson
import java.math.BigDecimal
import java.time.YearMonth
import java.time.ZonedDateTime
import java.util.*


fun vedtak(
    vedtakId: Long = 1,
    utbetalingsperioder: List<no.nav.etterlatte.domene.vedtak.Utbetalingsperiode> = listOf(
        no.nav.etterlatte.domene.vedtak.Utbetalingsperiode(
            id = 1,
            periode = Periode(fom = YearMonth.of(2022, 1), null),
            beloep = BigDecimal.valueOf(2000),
            type = UtbetalingsperiodeType.UTBETALING
        )
    )

): Vedtak = Vedtak(
    vedtakId = vedtakId,
    behandling = Behandling(
        id = UUID.randomUUID(),
        type = BehandlingType.FORSTEGANGSBEHANDLING
    ),
    sak = Sak(
        id = 1,
        ident = "12345678913",
        sakType = ""
    ),
    type = VedtakType.INNVILGELSE,
    virk = Periode(YearMonth.of(2022, 1), YearMonth.of(2022, 2)),
    grunnlag = emptyList(),
    vedtakFattet = VedtakFattet(
        ansvarligSaksbehandler = "12345678",
        ansvarligEnhet = "123",
        tidspunkt = ZonedDateTime.now()
    ),
    attestasjon = Attestasjon(
        attestant = "87654321",
        attesterendeEnhet = "123",
        tidspunkt = ZonedDateTime.now()
    ),
    pensjonTilUtbetaling = utbetalingsperioder,
    avkorting = null,
    beregning = null,
    vilkaarsvurdering = null
)

fun vedtakEvent(vedtak: Vedtak) = """
    {
      "@event_name": "vedtak_fattet",
      "@vedtak": ${vedtak.toJson()}
    }
"""