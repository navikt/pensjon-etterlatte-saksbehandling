package no.nav.etterlatte.avkorting.regler

import no.nav.etterlatte.avkorting.Aarsoppgjoer
import no.nav.etterlatte.avkorting.AvkortetYtelse
import no.nav.etterlatte.beregning.regler.omstillingstoenad.OMS_GYLDIG_FRA
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerGrenseDto
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerResultatType
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
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
import java.time.temporal.ChronoUnit

data class EtteroppgjoerDifferanseGrunnlag(
    val utbetaltStoenad: FaktumNode<Aarsoppgjoer>,
    val nyBruttoStoenad: FaktumNode<Aarsoppgjoer>,
)

val nyBruttoStoenad: Regel<EtteroppgjoerDifferanseGrunnlag, List<AvkortetYtelse>> =
    finnFaktumIGrunnlag(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "",
        finnFaktum = EtteroppgjoerDifferanseGrunnlag::nyBruttoStoenad,
        finnFelt = { it.avkortetYtelse },
    )

val utbetaltStoenad: Regel<EtteroppgjoerDifferanseGrunnlag, List<AvkortetYtelse>> =
    finnFaktumIGrunnlag(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "",
        finnFaktum = EtteroppgjoerDifferanseGrunnlag::utbetaltStoenad,
        finnFelt = { it.avkortetYtelse },
    )

val sumNyBruttoStoenad =
    RegelMeta(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "",
        regelReferanse = RegelReferanse(id = "REGEL-ETTEROPPGJOER-NY-BRUTTO-STOENAD"),
    ) benytter nyBruttoStoenad med { avkortetYtelse ->
        avkortetYtelse.sumOf {
            // TODO egen regel?
            (it.periode.fom.until(it.periode.tom, ChronoUnit.MONTHS) + 1) * it.ytelseEtterAvkorting
        }
    }

val sumUtbetaltStoenad =
    RegelMeta(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "",
        regelReferanse = RegelReferanse(id = "REGEL-ETTEROPPGJOER-UTBETALT-STOENAD"),
    ) benytter utbetaltStoenad med { avkortetYtelse ->
        avkortetYtelse.sumOf {
            // TODO egen regel?
            (it.periode.fom.until(it.periode.tom, ChronoUnit.MONTHS) + 1) * it.ytelseEtterAvkorting
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

        val etterbetaling = rettsgebyr.rettsgebyr
        val tilbakekreving = rettsgebyr.rettsgebyr.divide(4)

        EtteroppgjoerGrense(
            etterbetaling,
            tilbakekreving,
            rettsgebyr,
        )
    }

val beregneEtteroppgjoerRegel =
    RegelMeta(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "",
        regelReferanse = RegelReferanse(id = "REGEL-ETTEROPPGJOER-RESULTAT"),
    ) benytter etteroppgjoerDifferanse og etteroppgjoerGrense med { differanse, grenser ->
        val status =
            when {
                Beregningstall(differanse.differanse) > grenser.tilbakekreving -> EtteroppgjoerResultatType.TILBAKEKREVING
                Beregningstall(differanse.differanse * -1) > grenser.etterbetaling -> EtteroppgjoerResultatType.ETTERBETALING
                else -> EtteroppgjoerResultatType.IKKE_ETTEROPPGJOER
            }

        EtteroppgjoerRegelResultat(
            status,
            grenser,
            differanse,
        )
    }

data class EtteroppgjoerDifferanse(
    val differanse: Long,
    val utbetaltStoenad: Long,
    val nyBruttoStoenad: Long,
)

data class EtteroppgjoerRegelResultat(
    val resultatType: EtteroppgjoerResultatType,
    val grense: EtteroppgjoerGrense,
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
