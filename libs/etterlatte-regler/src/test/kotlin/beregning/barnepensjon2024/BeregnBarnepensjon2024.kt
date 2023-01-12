package no.nav.etterlatte.libs.regler.beregning.barnepensjon2024

import no.nav.etterlatte.libs.regler.RegelMeta
import no.nav.etterlatte.libs.regler.benytter
import no.nav.etterlatte.libs.regler.beregning.BarnepensjonGrunnlag
import no.nav.etterlatte.libs.regler.beregning.barnepensjon1967.BP_1967_DATO
import no.nav.etterlatte.libs.regler.beregning.barnepensjon1967.trygdetidsfaktor.trygdetidsFaktor
import no.nav.etterlatte.libs.regler.beregning.toDoRegelReferanse
import no.nav.etterlatte.libs.regler.definerKonstant
import no.nav.etterlatte.libs.regler.med
import no.nav.etterlatte.libs.regler.og
import java.math.BigDecimal
import java.time.LocalDate

val BP_2024_DATO: LocalDate = LocalDate.of(2024, 1, 1)

val beloepEnForelderDoed = definerKonstant<BarnepensjonGrunnlag, BigDecimal>(
    gjelderFra = BP_2024_DATO,
    beskrivelse = "Kronebelop benyttet for barn med en forelder død",
    regelReferanse = toDoRegelReferanse,
    verdi = BigDecimal(100_000)
)

val beregnBarnepensjon2024Regel = RegelMeta(
    gjelderFra = BP_1967_DATO,
    beskrivelse = "Reduserer ytelsen mot opptjening i folketrygden",
    regelReferanse = toDoRegelReferanse
) benytter beloepEnForelderDoed og trygdetidsFaktor med { sats, trygdetidsfaktor ->
    (sats * trygdetidsfaktor)
}