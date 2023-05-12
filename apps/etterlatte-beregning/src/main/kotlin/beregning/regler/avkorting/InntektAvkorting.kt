package no.nav.etterlatte.beregning.regler.avkorting

import no.nav.etterlatte.beregning.grunnbeloep.Grunnbeloep
import no.nav.etterlatte.beregning.grunnbeloep.GrunnbeloepRepository
import no.nav.etterlatte.beregning.regler.Beregningstall
import no.nav.etterlatte.beregning.regler.omstillingstoenad.OMS_GYLDIG_FROM_TEST
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

val opprundetInntekt = RegelMeta(
    gjelderFra = OMS_GYLDIG_FROM_TEST,
    beskrivelse = "Inntekt opprundet til nærmeste tusen",
    regelReferanse = RegelReferanse(id = "")
) benytter inntekt med { inntekt ->
    Beregningstall(inntekt).round(-3, Beregningstall.NAERMESTE_TUSEN)
}

val overstegetInntekt = RegelMeta(
    gjelderFra = OMS_GYLDIG_FROM_TEST,
    beskrivelse = "Finner oversteget inntekt",
    regelReferanse = RegelReferanse(id = "")
) benytter opprundetInntekt og grunnbeloep med { inntekt, grunnbeleop ->
    val halvtGrunnbeloep = Beregningstall(grunnbeleop.grunnbeloep).divide(2)
    inntekt
        .minus(halvtGrunnbeloep)
        .zeroIfNegative()
}

val avkortingFaktor = definerKonstant<InntektAvkortingGrunnlag, Beregningstall>(
    gjelderFra = OMS_GYLDIG_FROM_TEST,
    beskrivelse = "Faktor for inntektsavkorting",
    regelReferanse = RegelReferanse("TODO"),
    verdi = Beregningstall(0.45)
)

val inntektAvkorting = RegelMeta(
    gjelderFra = OMS_GYLDIG_FROM_TEST,
    beskrivelse = "Avkorter inntekt som har oversteget et halvt grunnbeløp med avkortingsfaktor",
    regelReferanse = RegelReferanse(id = "TODO")
) benytter overstegetInntekt og avkortingFaktor med { overstegetInntekt, avkortingFaktor ->
    avkortingFaktor.multiply(overstegetInntekt).divide(12).toInteger()
}