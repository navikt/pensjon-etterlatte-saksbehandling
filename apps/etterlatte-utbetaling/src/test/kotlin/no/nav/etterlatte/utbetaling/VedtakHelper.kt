package no.nav.etterlatte.utbetaling

import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.Regelverk
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.dbutils.Tidspunkt
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.sak.VedtakSak
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
                regelverk = Regelverk.REGELVERK_TOM_DES_2023,
            ),
        ),
    ident: String = "12345678913",
    sakId: SakId = sakId1,
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
            id = sakId1,
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
                        regelverk = Regelverk.REGELVERK_TOM_DES_2023,
                    ),
                ),
            opphoerFraOgMed = null,
        ),
)

fun attestertvedtakEvent(vedtakDto: VedtakDto) =
    """
    {
      "$EVENT_NAME_KEY": "${VedtakKafkaHendelseHendelseType.ATTESTERT.lagEventnameForType()}",
      "vedtak": ${vedtakDto.toJson()}
    }
"""
