package no.nav.etterlatte.avkorting

import no.nav.etterlatte.libs.common.beregning.AvkortingGrunnlagLagreDto
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import java.time.Month
import java.time.YearMonth

object AvkortingValider {
    fun validerInntekt(
        nyInntekt: AvkortingGrunnlagLagreDto,
        avkorting: Avkorting,
        erFoerstegangsbehandling: Boolean,
        naa: YearMonth = YearMonth.now(),
    ) {
        skalIkkeKunneEndreInntektITidligereAar(erFoerstegangsbehandling, nyInntekt.fom, avkorting, naa)

        foersteRevurderingIAareneEtterInnvilgelsesaarMaaStarteIJanuar(
            nyInntekt,
            avkorting,
            erFoerstegangsbehandling,
        )
        skalIkkeKunneLeggeTilEllerEndreAarsinntektTidligereEnnForrigeAarsinntekt(
            nyInntekt.fom,
            avkorting,
        )
        skalIkkeLeggeTilFratrekkInnAarHvisDetErEtFulltaar(
            nyInntekt,
            nyInntekt.fom,
        )

        // TODO valider at virk tidligere enn forrige innvilgelse ikke støttes enda
    }

    private fun skalIkkeKunneEndreInntektITidligereAar(
        erFoerstegangsbehandling: Boolean,
        nyInntektFom: YearMonth,
        avkorting: Avkorting,
        naa: YearMonth,
    ) {
        if (!erFoerstegangsbehandling && nyInntektFom.year < naa.year) {
            val sisteInntekt =
                (avkorting.aarsoppgjoer.single { it.aar == nyInntektFom.year } as AarsoppgjoerLoepende)
                    .inntektsavkorting
                    .maxBy { it.grunnlag.periode.fom }
                    .grunnlag
            val forrigeBehandlingErIkkeOpphoer = sisteInntekt.periode.tom == null
            // Hvis siste angitte inntekt har satt til og med betyr det at det var opphør og denne behandlingen er en gjenåpning.
            // Da må det være mulig og endre inntekten selv om det er et tidligere år
            if (forrigeBehandlingErIkkeOpphoer) {
                throw InntektForTidligereAar()
            }
        }
    }

    private fun foersteRevurderingIAareneEtterInnvilgelsesaarMaaStarteIJanuar(
        nyInntekt: AvkortingGrunnlagLagreDto,
        avkorting: Avkorting,
        erFoerstegangsbehandling: Boolean,
    ) {
        if (!erFoerstegangsbehandling) {
            if (avkorting.aarsoppgjoer.none { it.aar == nyInntekt.fom.year }) {
                if (nyInntekt.fom.month != Month.JANUARY) {
                    throw FoersteRevurderingSenereEnnJanuar()
                }
            }
        }
    }

    /*
    Det skal i utgangspunktet ikke være lov å endre inntekt bakover i tid. Skal alltid være måned etter rapportert endrng.
    Er det gjort feil av saksbehandler kan siste inntektsendring redigeres.
     */
    private fun skalIkkeKunneLeggeTilEllerEndreAarsinntektTidligereEnnForrigeAarsinntekt(
        fom: YearMonth,
        avkorting: Avkorting,
    ) {
        val nyligsteInntekt =
            (avkorting.aarsoppgjoer.singleOrNull { it.aar == fom.year } as AarsoppgjoerLoepende?)
                ?.inntektsavkorting
                ?.lastOrNull()
        if (nyligsteInntekt != null && nyligsteInntekt.grunnlag.periode.fom > fom) {
            throw IkkeTillattException(
                code = "NY_INNTEKT_KUN_NY_ELLER_NYLIGSTE",
                detail = "Kan ikke legge til eller endre årsinntekt som er tidligere enn forrige angitte årsinntekt.",
            )
        }
    }

    private fun skalIkkeLeggeTilFratrekkInnAarHvisDetErEtFulltaar(
        nyInntekt: AvkortingGrunnlagLagreDto,
        fom: YearMonth,
    ) {
        val fratrekkLagtTil = nyInntekt.fratrekkInnAar > 0 || nyInntekt.fratrekkInnAarUtland > 0
        if (fratrekkLagtTil && fom.month == Month.JANUARY) {
            throw HarFratrekkInnAarForFulltAar()
        }
    }
}

class FoersteRevurderingSenereEnnJanuar :
    IkkeTillattException(
        code = "FOERSTE_REVURDERING_I_NYTT_AAR_SENERE_ENN_JANUAR",
        detail = "Første revurdering i årene etter innvilgelsesår må være fom januar.",
    )

class HarFratrekkInnAarForFulltAar :
    IkkeTillattException(
        code = "NY_INNTEKT_FRATREKK_INN_AAR_FULLT_AAR",
        detail = "Kan ikke legge til fratrekk inn år når det er innvilga måned fra og med januar",
    )

class InntektForTidligereAar :
    IkkeTillattException(
        "ENDRE_INNTEKT_TIDLIGERE_AAR",
        "Det er ikke mulig å endre inntekt for tidligere år",
    )
