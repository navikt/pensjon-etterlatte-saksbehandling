package no.nav.etterlatte.avkorting

import no.nav.etterlatte.libs.common.beregning.AvkortingGrunnlagLagreDto
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import java.time.Month
import java.time.YearMonth

object AvkortingValider {
    fun validerInntekt(
        nyInntekt: AvkortingGrunnlagLagreDto,
        avkorting: Avkorting,
        innvilgelse: Boolean,
    ) {
        foersteRevurderingIAareneEtterInnvilgelsesaarMaaStarteIJanuar(
            nyInntekt,
            avkorting,
            innvilgelse,
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

    private fun foersteRevurderingIAareneEtterInnvilgelsesaarMaaStarteIJanuar(
        nyInntekt: AvkortingGrunnlagLagreDto,
        avkorting: Avkorting,
        innvilgelse: Boolean,
    ) {
        if (!innvilgelse) {
            if (avkorting.aarsoppgjoer.none { it.aar == nyInntekt.fom.year }) {
                if (nyInntekt.fom.month != Month.JANUARY) {
                    throw FoersteRevurderingSenereEnnJanuar()
                }
            }
        }
    }

    private fun skalIkkeKunneLeggeTilEllerEndreAarsinntektTidligereEnnForrigeAarsinntekt(
        fom: YearMonth,
        avkorting: Avkorting,
    ) {
        val nyligsteInntekt =
            avkorting.aarsoppgjoer
                .singleOrNull { it.aar == fom.year }
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

class RevurderingHarEndretFratrekkInnAar :
    IkkeTillattException(
        code = "NY_INNTEKT_FRATREKK_INN_AAR_REVURDERING",
        detail = "Skal ikke endre fratrekk inn år i en revurdering",
    )
