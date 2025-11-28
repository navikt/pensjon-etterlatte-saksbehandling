package no.nav.etterlatte.statistikk.domain

import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerResultatType
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.statistikk.database.EtteroppgjoerRad
import no.nav.etterlatte.statistikk.service.AktivitetForMaaned
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import kotlin.math.absoluteValue

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
    val sakId: SakId,
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
    val etteroppgjoerAar: Int?,
    val etteroppgjoerUtbetalt: Long?,
    val etteroppgjoerNyStoenad: Long?,
    val etteroppgjoerDifferanse: Long?,
    val etteroppgjoerResultat: String?,
    val etterbetaltBeloep: Long?,
    val tilbakekrevdBeloep: Long?,
)

private const val EN_G_PER_MND = 10336

class MaanedStatistikk(
    val maaned: YearMonth,
    stoenadRader: List<StoenadRad>,
    aktiviteter: Map<SakId, AktivitetForMaaned>,
    etteroppgjoer: Map<SakId, EtteroppgjoerRad>,
) {
    val rader: List<MaanedStoenadRad>

    init {
        val vedtakPerSak = stoenadRader.groupBy { it.sakId }

        rader =
            vedtakPerSak.mapNotNull { (sakId, vedtakPaaSak) ->
                val sisteVedtak = vedtakPaaSak.maxBy { it.tekniskTid }

                // finn aktuell beregnet ytelse for dette vedtaket
                val aktuellBeregning =
                    sisteVedtak.beregning?.let { beregning ->
                        beregning.beregningsperioder.find { it.datoFOM <= maaned && (it.datoTOM ?: maaned) >= maaned }
                    }
                val aktuellUtbetalingsperiode =
                    sisteVedtak.vedtaksperioder
                        ?.sortedByDescending { it.fraOgMed }
                        ?.find {
                            it.fraOgMed <= maaned && (it.tilOgMed ?: maaned) >= maaned
                        }
                val erOpphoer =
                    when (aktuellUtbetalingsperiode) {
                        null ->
                            sisteVedtak.vedtakType == VedtakType.OPPHOER ||
                                (sisteVedtak.opphoerFom != null && sisteVedtak.opphoerFom <= maaned)

                        else -> aktuellUtbetalingsperiode.type == StoenadPeriodeType.OPPHOER
                    }
                if (erOpphoer) {
                    return@mapNotNull null
                }
                val nettoYtelse =
                    aktuellBeregning?.utbetaltBeloep
                        ?: sisteVedtak.nettoYtelse?.toInt()
                        ?: 0

                val (avkortingsbeloep, aarsinntekt, sanksjon) =
                    sisteVedtak.avkorting?.let { avkorting ->
                        val aktuellAvkorting =
                            avkorting.avkortetYtelse.find { it.fom <= maaned && (it.tom ?: maaned) >= maaned }
                        val avkortingGrunnlag =
                            avkorting.avkortingGrunnlag.find { it.fom <= maaned && (it.tom ?: maaned) >= maaned }
                        val aarsinntekt = avkortingGrunnlag?.let { it.aarsinntekt - it.fratrekkInnAar }
                        Triple(
                            aktuellAvkorting?.avkortingsbeloep,
                            aarsinntekt,
                            aktuellAvkorting?.sanksjonertYtelse?.sanksjonType,
                        )
                    } ?: Triple(null, null, null)

                val aktivitetForMaaned =
                    (aktiviteter[sakId] ?: AktivitetForMaaned.FALLBACK_OMSTILLINGSSTOENAD)
                        .takeIf { sisteVedtak.sakYtelse == SakType.OMSTILLINGSSTOENAD.name }

                val sakYtelsesgruppe =
                    if (sisteVedtak.sakYtelse == SakType.OMSTILLINGSSTOENAD.name) {
                        sisteVedtak.sakYtelsesgruppe
                    } else {
                        when (sisteVedtak.sakYtelsesgruppe) {
                            SakYtelsesgruppe.FORELDRELOES -> SakYtelsesgruppe.FORELDRELOES
                            null, SakYtelsesgruppe.EN_AVDOED_FORELDER -> {
                                // Kan være foreldreløs med overstyrt beregning
                                if (sisteVedtak.beregning?.overstyrtBeregning == true &&
                                    (
                                        aktuellBeregning?.utbetaltBeloep
                                            ?: 0
                                    ) > EN_G_PER_MND
                                ) {
                                    SakYtelsesgruppe.FORELDRELOES
                                } else {
                                    SakYtelsesgruppe.EN_AVDOED_FORELDER
                                }
                            }
                        }
                    }

                val rad =
                    MaanedStoenadRad(
                        id = -1,
                        fnrSoeker = sisteVedtak.fnrSoeker,
                        fnrForeldre = sisteVedtak.fnrForeldre,
                        fnrSoesken = aktuellBeregning?.soeskenFlokk ?: sisteVedtak.fnrSoesken,
                        anvendtTrygdetid = aktuellBeregning?.trygdetid?.toString() ?: sisteVedtak.anvendtTrygdetid,
                        nettoYtelse = nettoYtelse.toString(),
                        avkortingsbeloep = avkortingsbeloep.toString(),
                        aarsinntekt = aarsinntekt.toString(),
                        beregningType = sisteVedtak.beregningType,
                        anvendtSats =
                            aktuellBeregning?.anvendtSats(
                                beregningstype = sisteVedtak.beregning.type,
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
                        vedtakLoependeTom = sisteVedtak.opphoerFom?.minusMonths(1)?.atEndOfMonth(),
                        statistikkMaaned = maaned,
                        sakUtland = sisteVedtak.sakUtland,
                        virkningstidspunkt = sisteVedtak.virkningstidspunkt,
                        utbetalingsdato = sisteVedtak.utbetalingsdato,
                        kilde = sisteVedtak.vedtaksloesning,
                        pesysId = sisteVedtak.pesysId,
                        sakYtelsesgruppe = sakYtelsesgruppe,
                        harAktivitetsplikt = aktivitetForMaaned?.harAktivitetsplikt?.name,
                        oppfyllerAktivitet = aktivitetForMaaned?.oppfyllerAktivitet,
                        aktivitet = aktivitetForMaaned?.aktivitet,
                        sanksjon = sanksjon?.name,
                        etteroppgjoerAar = null,
                        etteroppgjoerUtbetalt = null,
                        etteroppgjoerNyStoenad = null,
                        etteroppgjoerDifferanse = null,
                        etteroppgjoerResultat = null,
                        etterbetaltBeloep = null,
                        tilbakekrevdBeloep = null,
                    )
                when (val etteroppgjoerRad = etteroppgjoer[sakId]) {
                    null -> rad
                    else ->
                        rad.copy(
                            // Sette inn de aktuelle mappingene for etteroppgjøret her
                            etteroppgjoerAar = etteroppgjoerRad.aar,
                            etteroppgjoerUtbetalt = etteroppgjoerRad.utbetaltStoenad,
                            etteroppgjoerNyStoenad = etteroppgjoerRad.nyBruttoStoenad,
                            etteroppgjoerDifferanse = etteroppgjoerRad.differanse,
                            etteroppgjoerResultat = etteroppgjoerRad.resultatType?.name,
                            etterbetaltBeloep =
                                etteroppgjoerRad.differanse?.absoluteValue?.takeIf {
                                    etteroppgjoerRad.resultatType ==
                                        EtteroppgjoerResultatType.ETTERBETALING
                                },
                            tilbakekrevdBeloep =
                                etteroppgjoerRad.differanse?.absoluteValue?.takeIf {
                                    etteroppgjoerRad.resultatType ==
                                        EtteroppgjoerResultatType.TILBAKEKREVING
                                },
                        )
                }
            }
    }
}
