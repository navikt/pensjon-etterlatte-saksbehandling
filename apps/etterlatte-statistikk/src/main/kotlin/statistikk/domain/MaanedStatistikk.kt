package no.nav.etterlatte.statistikk.domain

import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import java.time.LocalDate
import java.time.YearMonth
import java.util.*

data class MaanedStoenadRad(
    val id: Long,
    val fnrSoeker: String,
    val fnrForeldre: List<String>,
    val fnrSoesken: List<String>,
    val anvendtTrygdetid: String,
    val nettoYtelse: String,
    val beregningType: String,
    val anvendtSats: String,
    val behandlingId: UUID,
    val sakId: Long,
    val sakNummer: Long,
    val tekniskTid: Tidspunkt,
    val sakYtelse: String,
    val versjon: String,
    val saksbehandler: String,
    val attestant: String?,
    val vedtakLoependeFom: LocalDate,
    val vedtakLoependeTom: LocalDate?,
    val statistikkMaaned: YearMonth
)

class MaanedStatistikk(val maaned: YearMonth, stoenadRader: List<StoenadRad>) {

    val rader: List<MaanedStoenadRad>

    init {
        val vedtakPerSak = stoenadRader.groupBy { it.sakId }

        rader = vedtakPerSak.mapNotNull { (_, vedtakPaaSak) ->
            val sisteVedtak = vedtakPaaSak.maxBy { it.tekniskTid }

            // finn aktuell utbetalingsperiode for dette vedtaket
            val aktuellPeriode = when (val beregning = sisteVedtak.beregning) {
                null -> null
                else -> beregning.beregningsperioder.find { it.datoFOM <= maaned && (it.datoTOM ?: maaned) >= maaned }
            }

            val nettoYtelse = aktuellPeriode?.utbetaltBeloep ?: sisteVedtak.nettoYtelse.toInt()
            val erOpphoer = when (sisteVedtak.vedtakType) {
                null -> nettoYtelse == 0
                else -> sisteVedtak.vedtakType == VedtakType.OPPHOER
            }

            var vedtakLoependeTom = sisteVedtak.vedtakLoependeTom
            if (erOpphoer) {
                val tidligsteVirk = vedtakPaaSak.minBy { it.vedtakLoependeFom }
                val foersteVirkMaaned =
                    tidligsteVirk.vedtakLoependeFom.let { YearMonth.of(it.year, it.month) }
                // Hvis denne saken har første virk og opphør i samme måned er den ikke relevant for statistikken
                if (maaned <= foersteVirkMaaned) {
                    return@mapNotNull null
                }
                // Hvis denne opphører denne måneden overstyr løpende tom til å være slutten av forrige måned
                vedtakLoependeTom = maaned.minusMonths(1).atEndOfMonth()
            }

            MaanedStoenadRad(
                id = -1,
                fnrSoeker = sisteVedtak.fnrSoeker,
                fnrForeldre = sisteVedtak.fnrForeldre,
                fnrSoesken = aktuellPeriode?.soeskenFlokk ?: sisteVedtak.fnrSoesken,
                anvendtTrygdetid = aktuellPeriode?.trygdetid?.toString() ?: sisteVedtak.anvendtTrygdetid,
                nettoYtelse = nettoYtelse.toString(),
                beregningType = sisteVedtak.beregningType,
                anvendtSats = aktuellPeriode?.grunnbelopMnd?.toString() ?: sisteVedtak.anvendtSats,
                behandlingId = sisteVedtak.behandlingId,
                sakId = sisteVedtak.sakId,
                sakNummer = sisteVedtak.sakNummer,
                tekniskTid = sisteVedtak.tekniskTid,
                sakYtelse = sisteVedtak.sakYtelse,
                versjon = sisteVedtak.versjon,
                saksbehandler = sisteVedtak.saksbehandler,
                attestant = sisteVedtak.attestant,
                vedtakLoependeFom = sisteVedtak.vedtakLoependeFom,
                vedtakLoependeTom = vedtakLoependeTom,
                statistikkMaaned = maaned
            )
        }
    }
}