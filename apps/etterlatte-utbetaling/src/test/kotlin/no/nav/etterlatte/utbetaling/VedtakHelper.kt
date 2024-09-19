package no.nav.etterlatte.utbetaling

import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.sak.VedtakSak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.Behandling
import no.nav.etterlatte.libs.common.vedtak.Periode
import no.nav.etterlatte.libs.common.vedtak.Utbetalingsperiode
import no.nav.etterlatte.libs.common.vedtak.UtbetalingsperiodeType
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.common.vedtak.VedtakInnholdDto
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseHendelseType
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import java.math.BigDecimal
import java.time.Month
import java.time.YearMonth
import java.util.UUID

fun vedtak(
    vedtakId: Long = 1,
    utbetalingsperioder: List<Utbetalingsperiode> =
        listOf(
            Utbetalingsperiode(
                id = 1,
                periode = Periode(fom = YearMonth.of(2022, 1), null),
                beloep = BigDecimal.valueOf(2000),
                type = UtbetalingsperiodeType.UTBETALING,
            ),
        ),
    ident: String = "12345678913",
    sakId: SakId = 1,
    behandling: Behandling =
        Behandling(
            id = UUID.randomUUID(),
            type = BehandlingType.FØRSTEGANGSBEHANDLING,
        ),
    saktype: SakType = SakType.BARNEPENSJON,
    virkningstidspunkt: YearMonth? = null,
    opphoerFraOgMed: YearMonth? = null,
) = VedtakDto(
    id = vedtakId,
    behandlingId = behandling.id,
    status = VedtakStatus.ATTESTERT,
    sak =
        VedtakSak(
            id = sakId,
            ident = ident,
            sakType = saktype,
        ),
    type = VedtakType.INNVILGELSE,
    vedtakFattet =
        VedtakFattet(
            ansvarligSaksbehandler = "12345678",
            ansvarligEnhet = Enhetsnummer("1234"),
            tidspunkt = Tidspunkt.now(),
        ),
    attestasjon =
        Attestasjon(
            attestant = "87654321",
            attesterendeEnhet = Enhetsnummer("1234"),
            tidspunkt = Tidspunkt.now(),
        ),
    innhold =
        VedtakInnholdDto.VedtakBehandlingDto(
            behandling = behandling,
            virkningstidspunkt = virkningstidspunkt ?: YearMonth.of(2022, 1),
            utbetalingsperioder = utbetalingsperioder,
            opphoerFraOgMed = opphoerFraOgMed,
        ),
)

fun ugyldigVedtakTilUtbetaling(
    vedtakId: Long = 1,
    behandling: Behandling =
        Behandling(
            id = UUID.randomUUID(),
            type = BehandlingType.FØRSTEGANGSBEHANDLING,
        ),
    saktype: SakType = SakType.BARNEPENSJON,
) = VedtakDto(
    id = vedtakId,
    behandlingId = behandling.id,
    status = VedtakStatus.ATTESTERT,
    sak =
        VedtakSak(
            id = 1,
            ident = "12345678913",
            sakType = saktype,
        ),
    type = VedtakType.INNVILGELSE,
    vedtakFattet = null,
    attestasjon =
        Attestasjon(
            attestant = "87654321",
            attesterendeEnhet = Enhetsnummer("1234"),
            tidspunkt = Tidspunkt.now(),
        ),
    innhold =
        VedtakInnholdDto.VedtakBehandlingDto(
            behandling = behandling,
            virkningstidspunkt = YearMonth.of(2022, 1),
            utbetalingsperioder =
                listOf(
                    Utbetalingsperiode(
                        1,
                        Periode(YearMonth.of(2022, 1), null),
                        BigDecimal.valueOf(1000),
                        UtbetalingsperiodeType.UTBETALING,
                    ),
                ),
            opphoerFraOgMed = null,
        ),
)

fun revurderingVedtak(
    vedtak: VedtakDto,
    behandling: Behandling =
        Behandling(
            id = UUID.randomUUID(),
            type = BehandlingType.REVURDERING,
        ),
    utbetalingsperioder: List<Utbetalingsperiode> =
        (vedtak.innhold as VedtakInnholdDto.VedtakBehandlingDto).let {
            listOf(
                Utbetalingsperiode(
                    id = it.utbetalingsperioder.last().id!! + 1,
                    periode =
                        Periode(
                            fom =
                                it.utbetalingsperioder
                                    .first()
                                    .periode.fom
                                    .plusMonths(1),
                            null,
                        ),
                    beloep =
                        BigDecimal(
                            it.utbetalingsperioder
                                .first()
                                .beloep!!
                                .longValueExact() - 1000,
                        ),
                    type = UtbetalingsperiodeType.UTBETALING,
                ),
            )
        },
) = VedtakDto(
    id = vedtak.id + 1,
    behandlingId = behandling.id,
    status = VedtakStatus.ATTESTERT,
    sak = vedtak.sak,
    type = VedtakType.ENDRING,
    vedtakFattet =
        VedtakFattet(
            ansvarligSaksbehandler = "12345678",
            ansvarligEnhet = Enhetsnummer("1234"),
            tidspunkt = Tidspunkt.now(),
        ),
    attestasjon =
        Attestasjon(
            attestant = "87654321",
            attesterendeEnhet = Enhetsnummer("1234"),
            tidspunkt = Tidspunkt.now(),
        ),
    innhold =
        VedtakInnholdDto.VedtakBehandlingDto(
            behandling = behandling,
            virkningstidspunkt = YearMonth.of(2022, 1),
            utbetalingsperioder = utbetalingsperioder,
            opphoerFraOgMed = null,
        ),
)

fun opphoersVedtak(
    vedtak: VedtakDto,
    behandling: Behandling =
        Behandling(
            id = UUID.randomUUID(),
            type = BehandlingType.REVURDERING,
        ),
) = VedtakDto(
    id = vedtak.id + 1,
    behandlingId = behandling.id,
    status = VedtakStatus.ATTESTERT,
    sak = vedtak.sak,
    type = VedtakType.OPPHOER,
    vedtakFattet =
        VedtakFattet(
            ansvarligSaksbehandler = "12345678",
            ansvarligEnhet = Enhetsnummer("1234"),
            tidspunkt = Tidspunkt.now(),
        ),
    attestasjon =
        Attestasjon(
            attestant = "87654321",
            attesterendeEnhet = Enhetsnummer("1234"),
            tidspunkt = Tidspunkt.now(),
        ),
    innhold =
        with(vedtak.innhold as VedtakInnholdDto.VedtakBehandlingDto) {
            VedtakInnholdDto.VedtakBehandlingDto(
                virkningstidspunkt = YearMonth.of(2022, 1),
                behandling = behandling,
                utbetalingsperioder =
                    listOf(
                        Utbetalingsperiode(
                            id = this.utbetalingsperioder.last().id!! + 1,
                            periode =
                                Periode(
                                    fom =
                                        this.utbetalingsperioder
                                            .first()
                                            .periode.fom
                                            .plusMonths(1),
                                    null,
                                ),
                            beloep = null,
                            type = UtbetalingsperiodeType.OPPHOER,
                        ),
                    ),
                opphoerFraOgMed =
                    this.utbetalingsperioder
                        .first()
                        .periode.fom
                        .plusMonths(1),
            )
        },
)

fun genererEtterfolgendeUtbetalingsperioder(
    antall: Int,
    intervallMnd: Int,
    forrigeId: Long,
    startPeriode: YearMonth,
    startBelop: BigDecimal,
) = (1..antall).map { index ->
    if (index < antall) {
        Utbetalingsperiode(
            id = forrigeId + index,
            periode =
                Periode(
                    fom = startPeriode.plusMonths(((index - 1) * intervallMnd).toLong()),
                    tom = startPeriode.plusMonths(((index * intervallMnd) - 1).toLong()),
                ),
            beloep = BigDecimal(startBelop.toLong() + index * 1000),
            type = UtbetalingsperiodeType.UTBETALING,
        )
    } else {
        Utbetalingsperiode(
            id = forrigeId + index,
            periode =
                Periode(
                    fom = startPeriode.plusMonths(((index - 1) * intervallMnd).toLong()),
                    tom = null,
                ),
            beloep = BigDecimal(startBelop.toLong() + index * 1000),
            type = UtbetalingsperiodeType.UTBETALING,
        )
    }
}

fun attestertvedtakEvent(vedtakDto: VedtakDto) =
    """
    {
      "$EVENT_NAME_KEY": "${VedtakKafkaHendelseHendelseType.ATTESTERT.lagEventnameForType()}",
      "vedtak": ${vedtakDto.toJson()}
    }
"""

fun main() {
    val initiellUtbetalingsperiode =
        Utbetalingsperiode(
            40,
            Periode(fom = YearMonth.of(2019, Month.JANUARY), tom = null),
            BigDecimal(1000),
            UtbetalingsperiodeType.UTBETALING,
        )
    val vedtak =
        vedtak(
            vedtakId = 40,
            utbetalingsperioder = listOf(initiellUtbetalingsperiode),
            ident = "16018222837",
            sakId = 15,
        )
    val vedtakEvent = attestertvedtakEvent(vedtak)

    val vedtakInnhold = (vedtak.innhold as VedtakInnholdDto.VedtakBehandlingDto)
    val revurderingsvedtak =
        revurderingVedtak(
            vedtak = vedtak,
            utbetalingsperioder =
                genererEtterfolgendeUtbetalingsperioder(
                    antall = 2,
                    intervallMnd = 6,
                    forrigeId = vedtakInnhold.utbetalingsperioder.last().id!!,
                    startPeriode =
                        vedtakInnhold.utbetalingsperioder
                            .last()
                            .periode.fom
                            .plusMonths(1),
                    startBelop = vedtakInnhold.utbetalingsperioder.last().beloep!!,
                ),
        )
    val revurderingsvedtakEvent = attestertvedtakEvent(revurderingsvedtak)

    val opphoersVedtak = attestertvedtakEvent(opphoersVedtak(revurderingsvedtak))

    // printer et vedtakevent og et revurderingsevent
    println("FORSTEGANGSBEHANDLING: ")
    println(vedtakEvent)
    println("REVURDERING: ")
    println(revurderingsvedtakEvent)
    println("OPPHOER: ")
    println(opphoersVedtak)
}
