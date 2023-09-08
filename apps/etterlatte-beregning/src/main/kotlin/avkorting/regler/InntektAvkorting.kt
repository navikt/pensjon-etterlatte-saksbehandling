package no.nav.etterlatte.avkorting.regler

import no.nav.etterlatte.beregning.regler.omstillingstoenad.OMS_GYLDIG_FROM_TEST
import no.nav.etterlatte.grunnbeloep.Grunnbeloep
import no.nav.etterlatte.grunnbeloep.GrunnbeloepRepository
import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.PeriodisertGrunnlag
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
import java.time.LocalDate
import java.util.*

data class InntektAvkortingGrunnlag(
    val inntekt: Beregningstall,
    val fratrekkInnUt: Beregningstall,
    val relevanteMaaneder: Beregningstall,
    val grunnlagId: UUID
)

data class InntektAvkortingGrunnlagWrapper(
    val inntektAvkortingGrunnlag: FaktumNode<InntektAvkortingGrunnlag>
)

data class PeriodisertInntektAvkortingGrunnlag(
    val periodisertInntektAvkortingGrunnlag: PeriodisertGrunnlag<FaktumNode<InntektAvkortingGrunnlag>>
) : PeriodisertGrunnlag<InntektAvkortingGrunnlagWrapper> {
    override fun finnAlleKnekkpunkter(): Set<LocalDate> {
        return periodisertInntektAvkortingGrunnlag.finnAlleKnekkpunkter()
    }

    override fun finnGrunnlagForPeriode(datoIPeriode: LocalDate): InntektAvkortingGrunnlagWrapper {
        return InntektAvkortingGrunnlagWrapper(
            periodisertInntektAvkortingGrunnlag.finnGrunnlagForPeriode(datoIPeriode)
        )
    }
}

val historiskeGrunnbeloep = GrunnbeloepRepository.historiskeGrunnbeloep.map { grunnbeloep ->
    val grunnbeloepGyldigFra = grunnbeloep.dato.atDay(1)
    definerKonstant<InntektAvkortingGrunnlagWrapper, Grunnbeloep>(
        gjelderFra = grunnbeloepGyldigFra,
        beskrivelse = "Grunnbeløp gyldig fra $grunnbeloepGyldigFra",
        regelReferanse = RegelReferanse(id = "REGEL-GRUNNBELOEP"),
        verdi = grunnbeloep
    )
}

val grunnbeloep: Regel<InntektAvkortingGrunnlagWrapper, Grunnbeloep> = RegelMeta(
    gjelderFra = OMS_GYLDIG_FROM_TEST,
    beskrivelse = "Finner grunnbeløp",
    regelReferanse = RegelReferanse(id = "REGEL-GRUNNBELOEP")
) velgNyesteGyldige historiskeGrunnbeloep

val inntektavkortingsgrunnlag: Regel<InntektAvkortingGrunnlagWrapper, InntektAvkortingGrunnlag> = finnFaktumIGrunnlag(
    gjelderFra = OMS_GYLDIG_FROM_TEST,
    beskrivelse = "Finner inntektsavkortingsgrunnlag",
    finnFaktum = InntektAvkortingGrunnlagWrapper::inntektAvkortingGrunnlag,
    finnFelt = { it }
)

val maanedsinntekt = RegelMeta(
    gjelderFra = OMS_GYLDIG_FROM_TEST,
    beskrivelse = "Inntekt for relevant periode nedrundet til nærmeste tusen oppdelt i relevante måneder",
    regelReferanse = RegelReferanse(id = "REGEL-NEDRUNDET-MÅNEDSINNTEKT")
) benytter inntektavkortingsgrunnlag med { inntektavkortingsgrunnlag ->
    val (inntekt, fratrekkInnUt, relevanteMaaneder) = inntektavkortingsgrunnlag
    inntekt.round(-3, RoundingMode.FLOOR).minus(fratrekkInnUt).divide(relevanteMaaneder)
}

val overstegetInntektPerMaaned = RegelMeta(
    gjelderFra = OMS_GYLDIG_FROM_TEST,
    beskrivelse = "Finner månedlig oversteget inntekt",
    regelReferanse = RegelReferanse(id = "REGEL-MÅNEDSINNTEKT-OVERSTEGET-HALV-G")
) benytter maanedsinntekt og grunnbeloep med { inntekt, grunnbeleop ->
    val halvtMaanedligGrunnbeloep = Beregningstall(grunnbeleop.grunnbeloep).divide(12).divide(2)
    inntekt
        .minus(halvtMaanedligGrunnbeloep)
        .zeroIfNegative()
}

val avkortingFaktor = definerKonstant<InntektAvkortingGrunnlagWrapper, Beregningstall>(
    gjelderFra = OMS_GYLDIG_FROM_TEST,
    beskrivelse = "Faktor for inntektsavkorting",
    regelReferanse = RegelReferanse("REGEL-FAKTOR-FOR-AVKORTING"),
    verdi = Beregningstall(0.45)
)

val inntektAvkorting = RegelMeta(
    gjelderFra = OMS_GYLDIG_FROM_TEST,
    beskrivelse = "Avkorter inntekt som har oversteget et halvt grunnbeløp med avkortingsfaktor",
    regelReferanse = RegelReferanse(id = "REGEL-INNTEKT-AVKORTING")
) benytter overstegetInntektPerMaaned og avkortingFaktor med { overstegetInntekt, avkortingFaktor ->
    avkortingFaktor.multiply(overstegetInntekt)
}

val kroneavrundetInntektAvkorting = RegelMeta(
    gjelderFra = OMS_GYLDIG_FROM_TEST,
    beskrivelse = "Gjør en kroneavrunding av intektavkorting",
    regelReferanse = RegelReferanse(id = "REGEL-KRONEAVRUNDING")
) benytter inntektAvkorting med { inntektAvkorting ->
    inntektAvkorting.round(decimals = 0).toInteger()
}