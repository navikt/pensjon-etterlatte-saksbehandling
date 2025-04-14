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
            val gjeldendeAar = avkorting.aarsoppgjoer.single { it.aar == nyInntektFom.year }
            if (gjeldendeAar is Etteroppgjoer) {
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
        val gjeldendeAar = avkorting.aarsoppgjoer.singleOrNull { it.aar == fom.year }
        when (gjeldendeAar) {
            is AarsoppgjoerLoepende -> {
                val nyligsteInntekt = gjeldendeAar?.inntektsavkorting?.lastOrNull()
                if (nyligsteInntekt != null && nyligsteInntekt.grunnlag.periode.fom > fom) {
                    throw IkkeTillattException(
                        code = "NY_INNTEKT_KUN_NY_ELLER_NYLIGSTE",
                        detail = "Kan ikke legge til eller endre årsinntekt som er tidligere enn forrige angitte årsinntekt.",
                    )
                }
            }

            is Etteroppgjoer -> throw IkkeTillattException(
                code = "ENDRE_INNTEKT_ETTEROPPGJOER",
                detail = "Kan ikke endre inntekt for etteroppgjort år",
            )

            null -> {}
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
