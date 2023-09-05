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
    val avkortingsbeloep: String,
    val aarsinntekt: String,
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
    val statistikkMaaned: YearMonth,
    val sakUtland: SakUtland?,
    val virkningstidspunkt: YearMonth?,
    val utbetalingsdato: LocalDate?
)

class MaanedStatistikk(val maaned: YearMonth, stoenadRader: List<StoenadRad>) {

    val rader: List<MaanedStoenadRad>

    init {
        val vedtakPerSak = stoenadRader.groupBy { it.sakId }

        rader = vedtakPerSak.mapNotNull { (_, vedtakPaaSak) ->
            val sisteVedtak = vedtakPaaSak.maxBy { it.tekniskTid }

            // finn aktuell beregnet ytelse for dette vedtaket
            val aktuellBeregnetYtelse = sisteVedtak.beregning?.let { beregning ->
                beregning.beregningsperioder.find { it.datoFOM <= maaned && (it.datoTOM ?: maaned) >= maaned }
            }
            val nettoYtelse = aktuellBeregnetYtelse?.utbetaltBeloep
                ?: sisteVedtak.nettoYtelse.toInt() // TODO nettoYtelse til skal utbedres?

            val (avkortingsbeloep, aarsinntekt) = sisteVedtak.avkorting?.let { avkorting ->
                val aktuellAvkorting =
                    avkorting.avkortetYtelse.find { it.fom <= maaned && (it.tom ?: maaned) >= maaned }
                val aktuellAarsinntekt =
                    avkorting.avkortingGrunnlag.find { it.fom <= maaned && (it.tom ?: maaned) >= maaned }
                Pair(aktuellAvkorting?.avkortingsbeloep, aktuellAarsinntekt?.aarsinntekt)
            } ?: Pair(null, null)

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
                fnrSoesken = aktuellBeregnetYtelse?.soeskenFlokk ?: sisteVedtak.fnrSoesken,
                anvendtTrygdetid = aktuellBeregnetYtelse?.trygdetid?.toString() ?: sisteVedtak.anvendtTrygdetid,
                nettoYtelse = nettoYtelse.toString(),
                avkortingsbeloep = avkortingsbeloep.toString(),
                aarsinntekt = aarsinntekt.toString(),
                beregningType = sisteVedtak.beregningType,
                anvendtSats = aktuellBeregnetYtelse?.grunnbelopMnd?.toString() ?: sisteVedtak.anvendtSats,
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
                statistikkMaaned = maaned,
                sakUtland = sisteVedtak.sakUtland,
                virkningstidspunkt = sisteVedtak.virkningstidspunkt,
                utbetalingsdato = sisteVedtak.utbetalingsdato
            )
        }
    }
}