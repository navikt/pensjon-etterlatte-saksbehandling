package no.nav.etterlatte.utbetaling

import no.nav.etterlatte.domene.vedtak.Attestasjon
import no.nav.etterlatte.domene.vedtak.Behandling
import no.nav.etterlatte.domene.vedtak.BehandlingType
import no.nav.etterlatte.domene.vedtak.Periode
import no.nav.etterlatte.domene.vedtak.Sak
import no.nav.etterlatte.domene.vedtak.Utbetalingsperiode
import no.nav.etterlatte.domene.vedtak.UtbetalingsperiodeType
import no.nav.etterlatte.domene.vedtak.Vedtak
import no.nav.etterlatte.domene.vedtak.VedtakFattet
import no.nav.etterlatte.domene.vedtak.VedtakType
import no.nav.etterlatte.libs.common.toJson
import java.math.BigDecimal
import java.time.Month
import java.time.YearMonth
import java.time.ZonedDateTime
import java.util.*


fun vedtak(
    vedtakId: Long = 1,
    utbetalingsperioder: List<Utbetalingsperiode> = listOf(
        Utbetalingsperiode(
            id = 1,
            periode = Periode(fom = YearMonth.of(2022, 1), null),
            beloep = BigDecimal.valueOf(2000),
            type = UtbetalingsperiodeType.UTBETALING
        )
    ),
    ident: String = "12345678913",
    sakId: Long = 1

): Vedtak = Vedtak(
    vedtakId = vedtakId,
    behandling = Behandling(
        id = UUID.randomUUID(),
        type = BehandlingType.FORSTEGANGSBEHANDLING
    ),
    sak = Sak(
        id = sakId,
        ident = ident,
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

fun ugyldigVedtakTilUtbetaling(
    vedtakId: Long = 1,
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
    pensjonTilUtbetaling = null,
    attestasjon = Attestasjon(
        attestant = "87654321",
        attesterendeEnhet = "123",
        tidspunkt = ZonedDateTime.now()
    ),
    avkorting = null,
    beregning = null,
    vilkaarsvurdering = null
)

fun revurderingVedtak(
    vedtak: Vedtak,
    nyttBeloep: BigDecimal = BigDecimal(vedtak.pensjonTilUtbetaling!!.first().beloep!!.longValueExact() - 1000),
    utbetalingsperioder: List<Utbetalingsperiode> = listOf(
        Utbetalingsperiode(
            id = vedtak.pensjonTilUtbetaling!!.last().id + 1,
            periode = Periode(fom = vedtak.pensjonTilUtbetaling!!.first().periode.fom.plusMonths(1), null),
            beloep = nyttBeloep,
            type = UtbetalingsperiodeType.UTBETALING
        )
    )
): Vedtak = Vedtak(
    vedtakId = vedtak.vedtakId + 1,
    behandling = Behandling(
        id = UUID.randomUUID(),
        type = BehandlingType.REVURDERING
    ),
    sak = vedtak.sak,
    type = VedtakType.ENDRING,
    virk = Periode(YearMonth.of(2022, 1), YearMonth.of(2022, 2)),
    grunnlag = emptyList(),
    vedtakFattet = VedtakFattet(
        ansvarligSaksbehandler = "12345678",
        ansvarligEnhet = "123",
        tidspunkt = ZonedDateTime.now()
    ),
    pensjonTilUtbetaling = utbetalingsperioder,
    attestasjon = Attestasjon(
        attestant = "87654321",
        attesterendeEnhet = "123",
        tidspunkt = ZonedDateTime.now()
    ),
    avkorting = null,
    beregning = null,
    vilkaarsvurdering = null
)

fun opphoersVedtak(
    vedtak: Vedtak
): Vedtak = Vedtak(
    vedtakId = vedtak.vedtakId + 1,
    behandling = Behandling(
        id = UUID.randomUUID(),
        type = BehandlingType.REVURDERING
    ),
    sak = vedtak.sak,
    type = VedtakType.OPPHOER,
    virk = Periode(YearMonth.of(2022, 1), YearMonth.of(2022, 2)),
    grunnlag = emptyList(),
    vedtakFattet = VedtakFattet(
        ansvarligSaksbehandler = "12345678",
        ansvarligEnhet = "123",
        tidspunkt = ZonedDateTime.now()
    ),
    pensjonTilUtbetaling = listOf(
        Utbetalingsperiode(
            id = vedtak.pensjonTilUtbetaling!!.last().id + 1,
            periode = Periode(fom = vedtak.pensjonTilUtbetaling!!.first().periode.fom.plusMonths(1), null),
            beloep = null,
            type = UtbetalingsperiodeType.OPPHOER
        )
    ),
    attestasjon = Attestasjon(
        attestant = "87654321",
        attesterendeEnhet = "123",
        tidspunkt = ZonedDateTime.now()
    ),
    avkorting = null,
    beregning = null,
    vilkaarsvurdering = null
)

fun genererEtterfolgendeUtbetalingsperioder(
    antall: Int,
    intervallMnd: Int,
    forrigeId: Long,
    startPeriode: YearMonth,
    startBelop: BigDecimal
) = (1..antall).mapIndexed() { index, _ ->
    if (index < antall) {
        Utbetalingsperiode(
            id = forrigeId + index,
            periode = Periode(
                fom = startPeriode.plusMonths(((index - 1) * intervallMnd).toLong()),
                tom = startPeriode.plusMonths(((index * intervallMnd) - 1).toLong()),
            ),
            beloep = BigDecimal(startBelop.toLong() + index * 1000),
            type = UtbetalingsperiodeType.UTBETALING
        )
    } else {
        Utbetalingsperiode(
            id = forrigeId + index,
            periode = Periode(
                fom = startPeriode.plusMonths(((index - 1) * intervallMnd).toLong()),
                tom = null,
            ),
            beloep = BigDecimal(startBelop.toLong() + index * 1000),
            type = UtbetalingsperiodeType.UTBETALING
        )
    }
}


fun vedtakEvent(vedtak: Vedtak) = """
    {
      "@event_name": "vedtak_fattet",
      "@vedtak": ${vedtak.toJson()}
    }
"""

fun main() {
    val initiellUtbetalingsperiode =
        Utbetalingsperiode(
            31,
            Periode(fom = YearMonth.of(2019, Month.JANUARY), tom = null),
            BigDecimal(1000),
            UtbetalingsperiodeType.UTBETALING
        )
    val vedtak = vedtak(
        vedtakId = 31,
        utbetalingsperioder = listOf(initiellUtbetalingsperiode),
        ident = "16017919184",
        sakId = 10
    )
    val vedtakEvent = vedtakEvent(vedtak)

    val revurderingsvedtak = revurderingVedtak(
        vedtak = vedtak,
        utbetalingsperioder = genererEtterfolgendeUtbetalingsperioder(
            antall = 4,
            intervallMnd = 6,
            forrigeId = vedtak.pensjonTilUtbetaling!!.last().id,
            startPeriode = vedtak.pensjonTilUtbetaling!!.last().periode.fom.plusMonths(1),
            startBelop = vedtak.pensjonTilUtbetaling!!.last().beloep!!
        )
    )
    val revurderingsvedtakEvent = vedtakEvent(revurderingsvedtak)

    // printer et vedtakevent og et revurderingsevent
    println("FORSTEGANGSBEHANDLING: ")
    println(vedtakEvent)
    println("REVURDERING: ")
    println(revurderingsvedtakEvent)


}