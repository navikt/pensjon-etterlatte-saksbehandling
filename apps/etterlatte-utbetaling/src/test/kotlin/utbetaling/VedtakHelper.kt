package no.nav.etterlatte.utbetaling

import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.Behandling
import no.nav.etterlatte.libs.common.vedtak.KafkaHendelseType
import no.nav.etterlatte.libs.common.vedtak.Periode
import no.nav.etterlatte.libs.common.vedtak.Utbetalingsperiode
import no.nav.etterlatte.libs.common.vedtak.UtbetalingsperiodeType
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.common.vedtak.VedtakType
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
    saktype: SakType = SakType.BARNEPENSJON

): VedtakDto = VedtakDto(
    vedtakId = vedtakId,
    behandling = behandling,
    sak = Sak(
        id = sakId,
        ident = ident,
        sakType = saktype
    ),
    type = VedtakType.INNVILGELSE,
    virkningstidspunkt = YearMonth.of(2022, 1),
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
    utbetalingsperioder = utbetalingsperioder
)

fun ugyldigVedtakTilUtbetaling(
    vedtakId: Long = 1,
    behandling: Behandling = Behandling(
        id = UUID.randomUUID(),
        type = BehandlingType.FØRSTEGANGSBEHANDLING
    ),
    saktype: SakType = SakType.BARNEPENSJON
): VedtakDto = VedtakDto(
    vedtakId = vedtakId,
    behandling = behandling,
    sak = Sak(
        id = 1,
        ident = "12345678913",
        sakType = saktype
    ),
    type = VedtakType.INNVILGELSE,
    virkningstidspunkt = YearMonth.of(2022, 1),
    vedtakFattet = null,
    utbetalingsperioder = listOf(
        Utbetalingsperiode(
            1,
            Periode(YearMonth.of(2022, 1), null),
            BigDecimal.valueOf(1000),
            UtbetalingsperiodeType.UTBETALING
        )
    ),
    attestasjon = Attestasjon(
        attestant = "87654321",
        attesterendeEnhet = "123",
        tidspunkt = ZonedDateTime.now()
    )
)

fun revurderingVedtak(
    vedtakDto: VedtakDto,
    nyttBeloep: BigDecimal = BigDecimal(vedtakDto.utbetalingsperioder.first().beloep!!.longValueExact() - 1000),
    utbetalingsperioder: List<Utbetalingsperiode> = listOf(
        Utbetalingsperiode(
            id = vedtakDto.utbetalingsperioder.last().id + 1,
            periode = Periode(fom = vedtakDto.utbetalingsperioder.first().periode.fom.plusMonths(1), null),
            beloep = nyttBeloep,
            type = UtbetalingsperiodeType.UTBETALING
        )
    )
): VedtakDto = VedtakDto(
    vedtakId = vedtakDto.vedtakId + 1,
    behandling = Behandling(
        id = UUID.randomUUID(),
        type = BehandlingType.REVURDERING
    ),
    sak = vedtakDto.sak,
    type = VedtakType.ENDRING,
    virkningstidspunkt = YearMonth.of(2022, 1),
    vedtakFattet = VedtakFattet(
        ansvarligSaksbehandler = "12345678",
        ansvarligEnhet = "123",
        tidspunkt = ZonedDateTime.now()
    ),
    utbetalingsperioder = utbetalingsperioder,
    attestasjon = Attestasjon(
        attestant = "87654321",
        attesterendeEnhet = "123",
        tidspunkt = ZonedDateTime.now()
    )
)

fun opphoersVedtak(
    vedtakDto: VedtakDto
): VedtakDto = VedtakDto(
    vedtakId = vedtakDto.vedtakId + 1,
    behandling = Behandling(
        id = UUID.randomUUID(),
        type = BehandlingType.REVURDERING
    ),
    sak = vedtakDto.sak,
    type = VedtakType.OPPHOER,
    virkningstidspunkt = YearMonth.of(2022, 1),
    vedtakFattet = VedtakFattet(
        ansvarligSaksbehandler = "12345678",
        ansvarligEnhet = "123",
        tidspunkt = ZonedDateTime.now()
    ),
    utbetalingsperioder = listOf(
        Utbetalingsperiode(
            id = vedtakDto.utbetalingsperioder!!.last().id + 1,
            periode = Periode(fom = vedtakDto.utbetalingsperioder!!.first().periode.fom.plusMonths(1), null),
            beloep = null,
            type = UtbetalingsperiodeType.OPPHOER
        )
    ),
    attestasjon = Attestasjon(
        attestant = "87654321",
        attesterendeEnhet = "123",
        tidspunkt = ZonedDateTime.now()
    )
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

fun vedtakEvent(vedtakDto: VedtakDto) = """
    {
      "@event_name": "${KafkaHendelseType.ATTESTERT}",
      "vedtak": ${vedtakDto.toJson()}
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
        vedtakDto = vedtak,
        utbetalingsperioder = genererEtterfolgendeUtbetalingsperioder(
            antall = 2,
            intervallMnd = 6,
            forrigeId = vedtak.utbetalingsperioder!!.last().id,
            startPeriode = vedtak.utbetalingsperioder!!.last().periode.fom.plusMonths(1),
            startBelop = vedtak.utbetalingsperioder!!.last().beloep!!
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