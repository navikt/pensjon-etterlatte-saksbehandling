package no.nav.etterlatte.statistikk.domain

import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.statistikk.service.AktivitetForMaaned
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

data class MaanedStoenadRad(
    val id: Long,
    val fnrSoeker: String,
    val fnrForeldre: List<String>,
    val fnrSoesken: List<String>,
    val anvendtTrygdetid: String,
    val nettoYtelse: String?,
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
    val utbetalingsdato: LocalDate?,
    val kilde: Vedtaksloesning,
    val pesysId: Long?,
    val sakYtelsesgruppe: SakYtelsesgruppe?,
    val harAktivitetsplikt: String?,
    val oppfyllerAktivitet: Boolean?,
    val aktivitet: String?,
    val sanksjon: String?,
)

class MaanedStatistikk(
    val maaned: YearMonth,
    stoenadRader: List<StoenadRad>,
    aktiviteter: Map<Long, AktivitetForMaaned>,
) {
    val rader: List<MaanedStoenadRad>

    init {
        val vedtakPerSak = stoenadRader.groupBy { it.sakId }

        rader =
            vedtakPerSak.mapNotNull { (sakId, vedtakPaaSak) ->
                val sisteVedtak = vedtakPaaSak.maxBy { it.tekniskTid }

                // finn aktuell beregnet ytelse for dette vedtaket
                val aktuellBeregnetYtelse =
                    sisteVedtak.beregning?.let { beregning ->
                        beregning.beregningsperioder.find { it.datoFOM <= maaned && (it.datoTOM ?: maaned) >= maaned }
                    }
                val nettoYtelse =
                    aktuellBeregnetYtelse?.utbetaltBeloep
                        ?: sisteVedtak.nettoYtelse?.toInt() // TODO nettoYtelse til skal utbedres?
                        ?: 0

                val (avkortingsbeloep, aarsinntekt, sanksjon) =
                    sisteVedtak.avkorting?.let { avkorting ->
                        val aktuellAvkorting =
                            avkorting.avkortetYtelse.find { it.fom <= maaned && (it.tom ?: maaned) >= maaned }
                        val avkortingGrunnlag =
                            avkorting.avkortingGrunnlag.find { it.fom <= maaned && (it.tom ?: maaned) >= maaned }
                        val aarsinntekt = avkortingGrunnlag?.let { it.aarsinntekt - it.fratrekkInnAar }
                        Triple(aktuellAvkorting?.avkortingsbeloep, aarsinntekt, aktuellAvkorting?.sanksjonertYtelse?.sanksjonType)
                    } ?: Triple(null, null, null)

                val erOpphoer =
                    when (sisteVedtak.vedtakType) {
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

                val aktivitetForMaaned =
                    (aktiviteter[sakId] ?: AktivitetForMaaned.FALLBACK_OMSTILLINGSSTOENAD)
                        .takeIf { sisteVedtak.sakYtelse == SakType.OMSTILLINGSSTOENAD.name }

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
                    anvendtSats =
                        aktuellBeregnetYtelse?.anvendtSats(
                            beregningstype = sisteVedtak.beregning.type,
                            // TODO: bytt ut med felt fra beregning når flere avdøde kommer inn
                            erForeldreloes = sisteVedtak.fnrForeldre.size > 1,
                            erOverstyrt = sisteVedtak.beregning.overstyrtBeregning == true,
                        ) ?: sisteVedtak.anvendtSats,
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
                    utbetalingsdato = sisteVedtak.utbetalingsdato,
                    kilde = sisteVedtak.kilde,
                    pesysId = sisteVedtak.pesysId,
                    sakYtelsesgruppe = sisteVedtak.sakYtelsesgruppe,
                    harAktivitetsplikt = aktivitetForMaaned?.harAktivitetsplikt?.name,
                    oppfyllerAktivitet = aktivitetForMaaned?.oppfyllerAktivitet,
                    aktivitet = aktivitetForMaaned?.aktivitet,
                    sanksjon = sanksjon?.name,
                )
            }
    }
}
