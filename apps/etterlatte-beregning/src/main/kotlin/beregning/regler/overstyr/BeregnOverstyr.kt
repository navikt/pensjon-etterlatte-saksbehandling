package no.nav.etterlatte.beregning.regler.overstyr

import no.nav.etterlatte.beregning.grunnlag.OverstyrBeregningGrunnlagData
import no.nav.etterlatte.beregning.regler.barnepensjon.BP_1967_DATO
import no.nav.etterlatte.beregning.regler.omstillingstoenad.OMS_GYLDIG_FRA
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
import java.time.LocalDate
import java.time.YearMonth

data class OverstyrGrunnlag(
    val overstyrGrunnlag: FaktumNode<OverstyrBeregningGrunnlagData>,
)

data class PeriodisertOverstyrGrunnlag(
    val overstyrGrunnlag: PeriodisertGrunnlag<FaktumNode<OverstyrBeregningGrunnlagData>>,
) : PeriodisertGrunnlag<OverstyrGrunnlag> {
    override fun finnAlleKnekkpunkter(): Set<LocalDate> {
        return overstyrGrunnlag.finnAlleKnekkpunkter()
    }

    override fun finnGrunnlagForPeriode(datoIPeriode: LocalDate): OverstyrGrunnlag {
        return OverstyrGrunnlag(
            overstyrGrunnlag.finnGrunnlagForPeriode(datoIPeriode),
        )
    }
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

data class ManuellBeregningReguleringGrunnlag(
    val virkningstidspunkt: FaktumNode<YearMonth>,
    val manueltBeregnetBeloep: FaktumNode<Beregningstall>,
)

val historiskeGrunnbeloepNy: Regel<ManuellBeregningReguleringGrunnlag, List<Grunnbeloep>> =
    definerKonstant(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "",
        regelReferanse = RegelReferanse(id = ""),
        verdi = GrunnbeloepRepository.historiskeGrunnbeloep,
    )

val reguleringsVirk: Regel<ManuellBeregningReguleringGrunnlag, YearMonth> =
    finnFaktumIGrunnlag(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "",
        finnFaktum = ManuellBeregningReguleringGrunnlag::virkningstidspunkt,
    ) { it }

val manueltBeregnetBeloep: Regel<ManuellBeregningReguleringGrunnlag, Beregningstall> =
    finnFaktumIGrunnlag(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "",
        finnFaktum = ManuellBeregningReguleringGrunnlag::manueltBeregnetBeloep,
    ) { it }

val toSiste: Regel<ManuellBeregningReguleringGrunnlag, Pair<Grunnbeloep, Grunnbeloep>> =
    RegelMeta(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "",
        regelReferanse = RegelReferanse(id = ""),
    ) benytter historiskeGrunnbeloepNy og reguleringsVirk med { grunnbeloep, virk ->
        val foerste = grunnbeloep.first { it.dato == virk.minusYears(1) }
        val andre = grunnbeloep.first { it.dato == virk }
        Pair(foerste, andre)
    }

val regulerOverstyrt =
    RegelMeta(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "",
        regelReferanse = RegelReferanse(id = ""),
    ) benytter toSiste og manueltBeregnetBeloep med { toSisteGrunnbeloep, beregnetBeloep ->
        val gammelG = Beregningstall(toSisteGrunnbeloep.first.grunnbeloep)
        val nyG = Beregningstall(toSisteGrunnbeloep.second.grunnbeloep)
        beregnetBeloep.multiply(nyG).divide(gammelG)
    }
