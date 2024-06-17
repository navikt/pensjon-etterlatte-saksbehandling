package no.nav.etterlatte.beregning.regler.overstyr

import no.nav.etterlatte.beregning.grunnlag.OverstyrBeregningGrunnlagData
import no.nav.etterlatte.beregning.regler.barnepensjon.BP_1967_DATO
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
import java.time.LocalDate

data class OverstyrGrunnlag(
    val overstyrGrunnlag: FaktumNode<OverstyrBeregningGrunnlagData>,
)

data class PeriodisertOverstyrGrunnlag(
    val overstyrGrunnlag: PeriodisertGrunnlag<FaktumNode<OverstyrBeregningGrunnlagData>>,
) : PeriodisertGrunnlag<OverstyrGrunnlag> {
    override fun finnAlleKnekkpunkter(): Set<LocalDate> = overstyrGrunnlag.finnAlleKnekkpunkter()

    override fun finnGrunnlagForPeriode(datoIPeriode: LocalDate): OverstyrGrunnlag =
        OverstyrGrunnlag(
            overstyrGrunnlag.finnGrunnlagForPeriode(datoIPeriode),
        )
}

val historiskeGrunnbeloep =
    GrunnbeloepRepository.historiskeGrunnbeloep.map { grunnbeloep ->
        val grunnbeloepGyldigFra = grunnbeloep.dato.atDay(1)
        definerKonstant<OverstyrGrunnlag, Grunnbeloep>(
            gjelderFra = grunnbeloepGyldigFra,
            beskrivelse = "Grunnbeløp gyldig fra $grunnbeloepGyldigFra",
            regelReferanse = RegelReferanse(id = "REGEL-HISTORISKE-GRUNNBELOEP"),
            verdi = grunnbeloep,
        )
    }

val grunnbeloep: Regel<OverstyrGrunnlag, Grunnbeloep> =
    RegelMeta(
        gjelderFra = BP_1967_DATO,
        beskrivelse = "Finner grunnbeløp",
        regelReferanse = RegelReferanse(id = "REGEL-GRUNNBELOEP"),
    ) velgNyesteGyldige historiskeGrunnbeloep

val overstyrtBeregning: Regel<OverstyrGrunnlag, OverstyrBeregningGrunnlagData> =
    finnFaktumIGrunnlag(
        gjelderFra = BP_1967_DATO,
        beskrivelse = "Overstyr beregning grunnlag",
        finnFaktum = OverstyrGrunnlag::overstyrGrunnlag,
        finnFelt = { it },
    )

val beregnOverstyrRegel =
    RegelMeta(
        gjelderFra = BP_1967_DATO,
        beskrivelse = "Overstyrt beregning",
        regelReferanse = RegelReferanse(id = "REGEL-OVERSTYR-GRUNNLAG"),
    ) benytter overstyrtBeregning og grunnbeloep med { overstyrBeregningGrunnlag, _ ->
        overstyrBeregningGrunnlag
    }
