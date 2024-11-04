package no.nav.etterlatte.statistikk.domain

import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.dbutils.Tidspunkt
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.vedtak.Utbetalingsperiode
import no.nav.etterlatte.libs.common.vedtak.UtbetalingsperiodeType
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

data class StoenadRad(
    val id: Long,
    val fnrSoeker: String,
    val fnrForeldre: List<String>,
    val fnrSoesken: List<String>,
    val anvendtTrygdetid: String,
    val nettoYtelse: String?,
    val beregningType: String,
    val anvendtSats: String,
    val behandlingId: UUID,
    val sakId: SakId,
    val sakNummer: Long,
    val tekniskTid: Tidspunkt,
    val sakYtelse: String,
    val versjon: String,
    val saksbehandler: String,
    val attestant: String?,
    val vedtakLoependeFom: LocalDate,
    val vedtakLoependeTom: LocalDate?,
    val beregning: Beregning?,
    val avkorting: Avkorting?,
    val vedtakType: VedtakType?,
    val sakUtland: SakUtland?,
    val virkningstidspunkt: YearMonth?,
    val utbetalingsdato: LocalDate?,
    val kilde: Vedtaksloesning,
    val pesysId: Long?,
    val sakYtelsesgruppe: SakYtelsesgruppe?,
    val opphoerFom: YearMonth?,
    val vedtaksperioder: List<StoenadUtbetalingsperiode>?,
)

enum class StoenadPeriodeType {
    OPPHOER,
    UTBETALING,
    ;

    companion object {
        fun fra(type: UtbetalingsperiodeType): StoenadPeriodeType =
            when (type) {
                UtbetalingsperiodeType.OPPHOER -> OPPHOER
                UtbetalingsperiodeType.UTBETALING -> UTBETALING
            }
    }
}

data class StoenadUtbetalingsperiode(
    val type: StoenadPeriodeType,
    val beloep: BigDecimal?,
    val fraOgMed: YearMonth,
    val tilOgMed: YearMonth?,
)

fun tilStoenadUtbetalingsperiode(utbetalingsperiode: Utbetalingsperiode): StoenadUtbetalingsperiode =
    StoenadUtbetalingsperiode(
        type = StoenadPeriodeType.fra(utbetalingsperiode.type),
        beloep = utbetalingsperiode.beloep,
        fraOgMed = utbetalingsperiode.periode.fom,
        tilOgMed = utbetalingsperiode.periode.tom,
    )
