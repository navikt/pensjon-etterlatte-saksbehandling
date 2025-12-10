package no.nav.etterlatte.avkorting.regler

import no.nav.etterlatte.avkorting.Aarsoppgjoer
import no.nav.etterlatte.avkorting.AvkortetYtelse
import no.nav.etterlatte.avkorting.FaktiskInntekt
import no.nav.etterlatte.beregning.regler.omstillingstoenad.OMS_GYLDIG_FRA
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerGrenseDto
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerResultatType
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.VedtakSamordningPeriode
import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.Regel
import no.nav.etterlatte.libs.regler.RegelMeta
import no.nav.etterlatte.libs.regler.RegelReferanse
import no.nav.etterlatte.libs.regler.benytter
import no.nav.etterlatte.libs.regler.definerKonstant
import no.nav.etterlatte.libs.regler.finnFaktumIGrunnlag
import no.nav.etterlatte.libs.regler.med
import no.nav.etterlatte.libs.regler.og
import no.nav.etterlatte.libs.regler.velgNyesteGyldige
import no.nav.etterlatte.regler.Beregningstall
import no.nav.etterlatte.rettsgebyr.RettsgebyrRepository
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.temporal.ChronoUnit

data class EtteroppgjoerDifferanseGrunnlag(
    val utbetaltStoenad: FaktumNode<List<VedtakSamordningPeriode>>,
    val nyBruttoStoenad: FaktumNode<Aarsoppgjoer>,
    val grunnlagForEtteroppgjoer: FaktumNode<FaktiskInntekt>,
    val harDoedsfall: FaktumNode<Boolean>,
)

val inntektIEtteroppgjoerAar: Regel<EtteroppgjoerDifferanseGrunnlag, FaktiskInntekt> =
    finnFaktumIGrunnlag(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "",
        finnFaktum = EtteroppgjoerDifferanseGrunnlag::grunnlagForEtteroppgjoer,
        finnFelt = { it },
    )

val harDoedsfall: Regel<EtteroppgjoerDifferanseGrunnlag, Boolean> =
    finnFaktumIGrunnlag(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "",
        finnFaktum = EtteroppgjoerDifferanseGrunnlag::harDoedsfall,
        finnFelt = { it },
    )

val harIngenInntekt =
    RegelMeta(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "",
        regelReferanse = RegelReferanse(id = "REGEL-ETTEROPPGJOER-INGEN-INNTEKT"),
    ) benytter inntektIEtteroppgjoerAar med { inntekt ->
        inntekt.afp == 0 &&
            inntekt.loennsinntekt == 0 &&
            inntekt.naeringsinntekt == 0 &&
            inntekt.utlandsinntekt == 0
    }

val nyBruttoStoenad: Regel<EtteroppgjoerDifferanseGrunnlag, List<AvkortetYtelse>> =
    finnFaktumIGrunnlag(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "",
        finnFaktum = EtteroppgjoerDifferanseGrunnlag::nyBruttoStoenad,
        finnFelt = { it.avkortetYtelse },
    )

val utbetaltStoenad: Regel<EtteroppgjoerDifferanseGrunnlag, List<VedtakSamordningPeriode>> =
    finnFaktumIGrunnlag(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "",
        finnFaktum = EtteroppgjoerDifferanseGrunnlag::utbetaltStoenad,
        finnFelt = { it },
    )

data class MaanederMedAvkortetYtelse(
    val antallMaaneder: Long,
    val ytelseEtterAvkorting: Int,
)

private fun normaliserPerioder(avkortingsperioder: List<AvkortetYtelse>): List<MaanederMedAvkortetYtelse> =
    avkortingsperioder.map {
        val tom = it.periode.tom ?: YearMonth.of(it.periode.fom.year, Month.DECEMBER)
        val antallMaaneder = it.periode.fom.until(tom, ChronoUnit.MONTHS) + 1
        MaanederMedAvkortetYtelse(
            antallMaaneder = antallMaaneder,
            ytelseEtterAvkorting = it.ytelseEtterAvkorting,
        )
    }

private fun normaliserVedtakPerioder(vedtakPerioder: List<VedtakSamordningPeriode>): List<MaanederMedAvkortetYtelse> =
    vedtakPerioder.map {
        val tom = it.tom ?: YearMonth.of(it.fom.year, Month.DECEMBER)
        val antallMaaneder = it.fom.until(tom, ChronoUnit.MONTHS) + 1
        MaanederMedAvkortetYtelse(
            antallMaaneder = antallMaaneder,
            ytelseEtterAvkorting = it.ytelseEtterAvkorting,
        )
    }

val normalisertNyBruttoStoenad: Regel<EtteroppgjoerDifferanseGrunnlag, List<MaanederMedAvkortetYtelse>> =
    RegelMeta(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "Gjør om en periode med avkortet ny brutto ytelse til en mellomverdi med antall måneder med avkortet ytelse",
        regelReferanse = RegelReferanse("REGEL-ETTEROPPGJOER-NORMALISER-NY-BRUTTO-STOENAD"),
    ) benytter nyBruttoStoenad med { avkorteYtelse ->
        normaliserPerioder(avkorteYtelse)
    }

val normalisertBruttoUtbetaltStoenad: Regel<EtteroppgjoerDifferanseGrunnlag, List<MaanederMedAvkortetYtelse>> =
    RegelMeta(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "Gjør om en periode med brutto avkortet ytelse til en mellomverdi med antall måneder med avkortet ytelse",
        regelReferanse = RegelReferanse("REGEL-ETTEROPPGJOER-NORMALISER-BRUTTO-UTBETALT-STOENAD"),
    ) benytter utbetaltStoenad med { vedtakPerioder ->
        normaliserVedtakPerioder(vedtakPerioder)
    }

val sumNyBruttoStoenad =
    RegelMeta(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "",
        regelReferanse = RegelReferanse(id = "REGEL-ETTEROPPGJOER-NY-BRUTTO-STOENAD"),
    ) benytter normalisertNyBruttoStoenad med { maanederMedAvkortetYtelse ->
        maanederMedAvkortetYtelse.sumOf {
            it.antallMaaneder * it.ytelseEtterAvkorting
        }
    }

val sumUtbetaltStoenad =
    RegelMeta(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "",
        regelReferanse = RegelReferanse(id = "REGEL-ETTEROPPGJOER-UTBETALT-STOENAD"),
    ) benytter normalisertBruttoUtbetaltStoenad med { maanederMedAvkortetYtelse ->
        maanederMedAvkortetYtelse.sumOf {
            it.antallMaaneder * it.ytelseEtterAvkorting
        }
    }

val etteroppgjoerDifferanse =
    RegelMeta(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "",
        regelReferanse = RegelReferanse(id = "REGEL-ETTEROPPGJOER-DIFFERANSE"),
    ) benytter sumUtbetaltStoenad og sumNyBruttoStoenad med { sumUtbetalt, sumNyBrutto ->
        val differanse = sumUtbetalt - sumNyBrutto
        EtteroppgjoerDifferanse(differanse, sumUtbetalt, sumNyBrutto)
    }

val finnHistoriskeRettsgebyr =
    RettsgebyrRepository.historiskeRettsgebyr.map { rettsgebyr ->
        val rettsgebyrGyldigFra = rettsgebyr.gyldigFra
        definerKonstant<EtteroppgjoerDifferanseGrunnlag, EtteroppgjoerRettsgebyr>(
            gjelderFra = rettsgebyrGyldigFra,
            beskrivelse = "Rettsgebyr gyldig fra $rettsgebyrGyldigFra",
            regelReferanse = RegelReferanse(id = "REGEL-HISTORISKE-RETTSGEBYR"),
            verdi = rettsgebyr,
        )
    }

val etteroppgjoerRettsgebyr: Regel<EtteroppgjoerDifferanseGrunnlag, EtteroppgjoerRettsgebyr> =
    RegelMeta(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "Finner rettsgebyr",
        regelReferanse = RegelReferanse(id = "REGEL-RETTSGEBYR"),
    ) velgNyesteGyldige finnHistoriskeRettsgebyr

val etteroppgjoerGrense =
    RegelMeta(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "Sette rettsgebyr",
        regelReferanse = RegelReferanse(id = "REGEL-ETTEROPPGJOER-GRENSE"),
    ) benytter etteroppgjoerRettsgebyr med { rettsgebyr ->

        val tilbakekreving = rettsgebyr.rettsgebyr
        val etterbetaling = rettsgebyr.rettsgebyr.divide(4)

        EtteroppgjoerGrense(
            tilbakekreving,
            etterbetaling,
            rettsgebyr,
        )
    }

val beregneEtteroppgjoerRegel =
    RegelMeta(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "",
        regelReferanse = RegelReferanse(id = "REGEL-ETTEROPPGJOER-RESULTAT"),
    ) benytter etteroppgjoerDifferanse og etteroppgjoerGrense og harIngenInntekt med { differanse, grenser, ingenInntekt ->
        val status =
            when {
                Beregningstall(differanse.differanse) > grenser.tilbakekreving -> EtteroppgjoerResultatType.TILBAKEKREVING
                Beregningstall(differanse.differanse * -1) > grenser.etterbetaling -> EtteroppgjoerResultatType.ETTERBETALING
                differanse.differanse == 0L && differanse.utbetaltStoenad == 0L &&
                    differanse.nyBruttoStoenad == 0L -> EtteroppgjoerResultatType.INGEN_ENDRING_UTEN_UTBETALING

                else -> EtteroppgjoerResultatType.INGEN_ENDRING_MED_UTBETALING
            }

        EtteroppgjoerRegelResultat(
            resultatType = status,
            grense = grenser,
            harIngenInntekt = ingenInntekt,
            differanse = differanse,
            opprinneligResultatType = null,
        )
    }

val beregneEtteroppgjoerRegelMedDoedsfall =
    RegelMeta(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "",
        regelReferanse = RegelReferanse(id = "REGEL-ETTEROPPGJOER-RESULTAT-MED-DOEDSFALL"),
    ) benytter beregneEtteroppgjoerRegel og harDoedsfall med { resultat, doedsfall ->

        if (doedsfall) {
            val overstyrtResultat =
                when (resultat.resultatType) {
                    EtteroppgjoerResultatType.TILBAKEKREVING -> EtteroppgjoerResultatType.INGEN_ENDRING_MED_UTBETALING
                    else -> resultat.resultatType
                }

            resultat.copy(resultatType = overstyrtResultat, opprinneligResultatType = resultat.resultatType)
        } else {
            resultat
        }
    }

data class EtteroppgjoerDifferanse(
    val differanse: Long,
    val utbetaltStoenad: Long,
    val nyBruttoStoenad: Long,
)

data class EtteroppgjoerRegelResultat(
    val resultatType: EtteroppgjoerResultatType,
    val opprinneligResultatType: EtteroppgjoerResultatType?,
    val grense: EtteroppgjoerGrense,
    val harIngenInntekt: Boolean,
    val differanse: EtteroppgjoerDifferanse,
    val tidspunkt: Tidspunkt = Tidspunkt.now(),
)

data class EtteroppgjoerGrense(
    val tilbakekreving: Beregningstall,
    val etterbetaling: Beregningstall,
    val rettsgebyr: EtteroppgjoerRettsgebyr,
) {
    fun toDto(): EtteroppgjoerGrenseDto =
        EtteroppgjoerGrenseDto(
            tilbakekreving = this.tilbakekreving.toDouble(),
            etterbetaling = this.etterbetaling.toDouble(),
            rettsgebyr = this.rettsgebyr.rettsgebyr.toInteger(),
            rettsgebyrGyldigFra = this.rettsgebyr.gyldigFra,
        )
}

data class EtteroppgjoerRettsgebyr(
    val gyldigFra: LocalDate,
    val rettsgebyr: Beregningstall,
)
