package no.nav.etterlatte.utbetaling

import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.Behandling
import no.nav.etterlatte.libs.common.vedtak.KafkaHendelseType
import no.nav.etterlatte.libs.common.vedtak.Periode
import no.nav.etterlatte.libs.common.vedtak.Utbetalingsperiode
import no.nav.etterlatte.libs.common.vedtak.UtbetalingsperiodeType
import no.nav.etterlatte.libs.common.vedtak.Vedtak
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Saktype
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
    sakId: Long = 1,
    behandling: Behandling = Behandling(
        id = UUID.randomUUID(),
        type = BehandlingType.FØRSTEGANGSBEHANDLING
    ),
    saktype: Saktype = Saktype.BARNEPENSJON

): Vedtak = Vedtak(
    vedtakId = vedtakId,
    behandling = behandling,
    sak = Sak(
        id = sakId,
        ident = ident,
        sakType = saktype.name
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
    beregning = null,
    vilkaarsvurdering = null
)

fun ugyldigVedtakTilUtbetaling(
    vedtakId: Long = 1,
    behandling: Behandling = Behandling(
        id = UUID.randomUUID(),
        type = BehandlingType.FØRSTEGANGSBEHANDLING
    ),
    saktype: Saktype = Saktype.BARNEPENSJON
): Vedtak = Vedtak(
    vedtakId = vedtakId,
    behandling = behandling,
    sak = Sak(
        id = 1,
        ident = "12345678913",
        sakType = saktype.name
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
    beregning = null,
    vilkaarsvurdering = null
)

fun genererEtterfolgendeUtbetalingsperioder(
    antall: Int,
    intervallMnd: Int,
    forrigeId: Long,
    startPeriode: YearMonth,
    startBelop: BigDecimal
) = (1..antall).map { index ->
    if (index < antall) {
        Utbetalingsperiode(
            id = forrigeId + index,
            periode = Periode(
                fom = startPeriode.plusMonths(((index - 1) * intervallMnd).toLong()),
                tom = startPeriode.plusMonths(((index * intervallMnd) - 1).toLong())
            ),
            beloep = BigDecimal(startBelop.toLong() + index * 1000),
            type = UtbetalingsperiodeType.UTBETALING
        )
    } else {
        Utbetalingsperiode(
            id = forrigeId + index,
            periode = Periode(
                fom = startPeriode.plusMonths(((index - 1) * intervallMnd).toLong()),
                tom = null
            ),
            beloep = BigDecimal(startBelop.toLong() + index * 1000),
            type = UtbetalingsperiodeType.UTBETALING
        )
    }
}

fun vedtakEvent(vedtak: Vedtak) = """
    {
      "@event_name": "${KafkaHendelseType.ATTESTERT}",
      "vedtak": ${vedtak.toJson()}
    }
"""

fun main() {
    val initiellUtbetalingsperiode =
        Utbetalingsperiode(
            40,
            Periode(fom = YearMonth.of(2019, Month.JANUARY), tom = null),
            BigDecimal(1000),
            UtbetalingsperiodeType.UTBETALING
        )
    val vedtak = vedtak(
        vedtakId = 40,
        utbetalingsperioder = listOf(initiellUtbetalingsperiode),
        ident = "16018222837",
        sakId = 15
    )
    val vedtakEvent = vedtakEvent(vedtak)

    val revurderingsvedtak = revurderingVedtak(
        vedtak = vedtak,
        utbetalingsperioder = genererEtterfolgendeUtbetalingsperioder(
            antall = 2,
            intervallMnd = 6,
            forrigeId = vedtak.pensjonTilUtbetaling!!.last().id,
            startPeriode = vedtak.pensjonTilUtbetaling!!.last().periode.fom.plusMonths(1),
            startBelop = vedtak.pensjonTilUtbetaling!!.last().beloep!!
        )
    )
    val revurderingsvedtakEvent = vedtakEvent(revurderingsvedtak)

    val opphoersVedtak = vedtakEvent(opphoersVedtak(revurderingsvedtak))

    // printer et vedtakevent og et revurderingsevent
    println("FORSTEGANGSBEHANDLING: ")
    println(vedtakEvent)
    println("REVURDERING: ")
    println(revurderingsvedtakEvent)
    println("OPPHOER: ")
    println(opphoersVedtak)
}