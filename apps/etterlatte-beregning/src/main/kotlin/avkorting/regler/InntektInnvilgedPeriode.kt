package no.nav.etterlatte.avkorting.regler

import no.nav.etterlatte.beregning.regler.omstillingstoenad.OMS_GYLDIG_FRA
import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.Regel
import no.nav.etterlatte.libs.regler.RegelMeta
import no.nav.etterlatte.libs.regler.RegelReferanse
import no.nav.etterlatte.libs.regler.benytter
import no.nav.etterlatte.libs.regler.finnFaktumIGrunnlag
import no.nav.etterlatte.libs.regler.med
import no.nav.etterlatte.regler.Beregningstall

data class ForventetInntektGrunnlag(
    val inntektTom: Beregningstall,
    val fratrekkInnAar: Beregningstall,
    val inntektUtlandTom: Beregningstall,
    val fratrekkInnAarUtland: Beregningstall,
)

/*
data class ForventetInntektGrunnlagWrapper(
    val inntektAvkortingGrunnlag: FaktumNode<ForventetInntektGrunnlag>,
)
*/

val forventetInntektGrunnlag: Regel<ForventetInntektGrunnlag, ForventetInntektGrunnlag> =
    finnFaktumIGrunnlag(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "",
        finnFaktum = { FaktumNode(it, kilde = "", beskrivelse = "") },
        finnFelt = { it },
    )

val forventetInntektInnvilgetPeriode =
    RegelMeta(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "",
        regelReferanse = RegelReferanse(id = "", versjon = "1.1"), // TODO
    ) benytter forventetInntektGrunnlag med { inntektavkortingsgrunnlag ->
        val (inntektTom, fratrekkInnAar, inntektutlandTom, fratrekkInnAarUtland) = inntektavkortingsgrunnlag
        inntektTom
            .minus(fratrekkInnAar)
            .plus(inntektutlandTom)
            .minus(fratrekkInnAarUtland)
        // .round(ANTALL_DESIMALER_INNTENKT, roundingModeInntekt) TODO skal nedrunding skje her?
    }
