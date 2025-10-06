package no.nav.etterlatte.avkorting.regler

import no.nav.etterlatte.beregning.regler.omstillingstoenad.OMS_GYLDIG_FRA
import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.Regel
import no.nav.etterlatte.libs.regler.RegelMeta
import no.nav.etterlatte.libs.regler.RegelReferanse
import no.nav.etterlatte.libs.regler.benytter
import no.nav.etterlatte.libs.regler.finnFaktumIGrunnlag
import no.nav.etterlatte.libs.regler.med
import no.nav.etterlatte.regler.ANTALL_DESIMALER_INNTENKT
import no.nav.etterlatte.regler.Beregningstall
import no.nav.etterlatte.regler.roundingModeInntekt

data class ForventetInntektGrunnlag(
    val inntektTom: Beregningstall,
    val fratrekkInnAar: Beregningstall,
    val inntektUtlandTom: Beregningstall,
    val fratrekkInnAarUtland: Beregningstall,
)

data class FaktiskInntektGrunnlag(
    val loennsinntekt: Beregningstall,
    val afp: Beregningstall,
    val naeringsinntekt: Beregningstall,
    val utland: Beregningstall,
)

val forventetInntektGrunnlag: Regel<FaktumNode<ForventetInntektGrunnlag>, ForventetInntektGrunnlag> =
    finnFaktumIGrunnlag(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "Finner grunnlag for inntekt innvilget periode",
        finnFaktum = { it },
        finnFelt = { it },
    )

val forventetInntektInnvilgetPeriode =
    RegelMeta(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "Beregner forventet inntekt i innvilget periode og runder ned til nærmeste tusen",
        regelReferanse = RegelReferanse(id = "REGEL-INNTEKT-INNVILGET-NEDRUNDET", versjon = "1.1"),
    ) benytter forventetInntektGrunnlag med { inntektavkortingsgrunnlag ->
        val (inntektTom, fratrekkInnAar, inntektutlandTom, fratrekkInnAarUtland) = inntektavkortingsgrunnlag
        inntektTom
            .minus(fratrekkInnAar)
            .plus(inntektutlandTom)
            .minus(fratrekkInnAarUtland)
            .round(ANTALL_DESIMALER_INNTENKT, roundingModeInntekt)
    }

val faktiskInntektGrunnlag: Regel<FaktumNode<FaktiskInntektGrunnlag>, FaktiskInntektGrunnlag> =
    finnFaktumIGrunnlag(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "Finner grunnlag for faktisk inntekt",
        finnFaktum = { it },
        finnFelt = { it },
    )

val faktiskInntektInnvilgetPeriode =
    RegelMeta(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "Beregner faktisk inntekt i innvilget periode",
        regelReferanse = RegelReferanse(id = "REGEL-FAKTISK-INNTEKT", versjon = "1.2"),
    ) benytter faktiskInntektGrunnlag med { grunnlagFaktiskInntekt ->
        grunnlagFaktiskInntekt.loennsinntekt
            .plus(grunnlagFaktiskInntekt.afp)
            .plus(grunnlagFaktiskInntekt.naeringsinntekt)
            .plus(grunnlagFaktiskInntekt.utland)
            .round(ANTALL_DESIMALER_INNTENKT, roundingModeInntekt)
    }
