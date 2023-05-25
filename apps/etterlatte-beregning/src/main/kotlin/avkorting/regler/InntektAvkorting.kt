package no.nav.etterlatte.avkorting.regler

import no.nav.etterlatte.beregning.regler.omstillingstoenad.OMS_GYLDIG_FROM_TEST
import no.nav.etterlatte.grunnbeloep.Grunnbeloep
import no.nav.etterlatte.grunnbeloep.GrunnbeloepRepository
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
import java.math.RoundingMode

data class InntektAvkortingGrunnlag(
    val inntekt: FaktumNode<Int>
)

val historiskeGrunnbeloep = GrunnbeloepRepository.historiskeGrunnbeloep.map { grunnbeloep ->
    val grunnbeloepGyldigFra = grunnbeloep.dato.atDay(1)
    definerKonstant<InntektAvkortingGrunnlag, Grunnbeloep>(
        gjelderFra = grunnbeloepGyldigFra,
        beskrivelse = "Grunnbeløp gyldig fra $grunnbeloepGyldigFra",
        regelReferanse = RegelReferanse(id = "REGEL-GRUNNBELOEP"),
        verdi = grunnbeloep
    )
}

val grunnbeloep: Regel<InntektAvkortingGrunnlag, Grunnbeloep> = RegelMeta(
    gjelderFra = OMS_GYLDIG_FROM_TEST,
    beskrivelse = "Finner grunnbeløp",
    regelReferanse = RegelReferanse(id = "REGEL-GRUNNBELOEP")
) velgNyesteGyldige historiskeGrunnbeloep

val inntekt: Regel<InntektAvkortingGrunnlag, Int> = finnFaktumIGrunnlag(
    gjelderFra = OMS_GYLDIG_FROM_TEST,
    beskrivelse = "Finner inntekt",
    finnFaktum = InntektAvkortingGrunnlag::inntekt,
    finnFelt = { it }
)

val nedrundetInntekt = RegelMeta(
    gjelderFra = OMS_GYLDIG_FROM_TEST,
    beskrivelse = "Inntekt nedrundet til nærmeste tusen",
    regelReferanse = RegelReferanse(id = "REGEL-NEDRUNDET-INNTEKT")
) benytter inntekt med { inntekt ->
    Beregningstall(inntekt).round(-3, RoundingMode.FLOOR)
}

val overstegetInntekt = RegelMeta(
    gjelderFra = OMS_GYLDIG_FROM_TEST,
    beskrivelse = "Finner oversteget inntekt",
    regelReferanse = RegelReferanse(id = "REGEL-INNTEKT-OVERSTEGET-HALV-G")
) benytter nedrundetInntekt og grunnbeloep med { inntekt, grunnbeleop ->
    val halvtGrunnbeloep = Beregningstall(grunnbeleop.grunnbeloep).divide(2)
    inntekt
        .minus(halvtGrunnbeloep)
        .zeroIfNegative()
}

val avkortingFaktor = definerKonstant<InntektAvkortingGrunnlag, Beregningstall>(
    gjelderFra = OMS_GYLDIG_FROM_TEST,
    beskrivelse = "Faktor for inntektsavkorting",
    regelReferanse = RegelReferanse("REGEL-FAKTOR-FOR-AVKORTING"),
    verdi = Beregningstall(0.45)
)

val inntektAvkorting = RegelMeta(
    gjelderFra = OMS_GYLDIG_FROM_TEST,
    beskrivelse = "Avkorter inntekt som har oversteget et halvt grunnbeløp med avkortingsfaktor",
    regelReferanse = RegelReferanse(id = "REGEL-INNTEKT-AVKORTING")
) benytter overstegetInntekt og avkortingFaktor med { overstegetInntekt, avkortingFaktor ->
    avkortingFaktor.multiply(overstegetInntekt).divide(12)
}

val kroneavrundetInntektAvkorting = RegelMeta(
    gjelderFra = OMS_GYLDIG_FROM_TEST,
    beskrivelse = "Gjør en kroneavrunding av intektavkorting",
    regelReferanse = RegelReferanse(id = "REGEL-KRONEAVRUNDING")
) benytter inntektAvkorting med { inntektAvkorting ->
    inntektAvkorting.round(decimals = 0).toInteger()
}