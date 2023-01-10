package no.nav.etterlatte.libs.regler.beregning.barnepensjon1967.barnekull

import no.nav.etterlatte.libs.regler.Regel
import no.nav.etterlatte.libs.regler.RegelMeta
import no.nav.etterlatte.libs.regler.ToDoRegelReferanse
import no.nav.etterlatte.libs.regler.beregning.BarnepensjonGrunnlag
import no.nav.etterlatte.libs.regler.beregning.barnepensjon1967.BP_1967_DATO
import no.nav.etterlatte.libs.regler.definerKonstant
import no.nav.etterlatte.libs.regler.finnFaktumIGrunnlag
import no.nav.etterlatte.libs.regler.kombinerer
import no.nav.etterlatte.libs.regler.med
import no.nav.etterlatte.libs.regler.multipliser
import no.nav.etterlatte.libs.regler.og
import java.math.BigDecimal

private val grunnbeloep: Regel<BarnepensjonGrunnlag, BigDecimal> =
    finnFaktumIGrunnlag(
        gjelderFra = BP_1967_DATO,
        beskrivelse = "Finner grunnbeløp",
        regelReferanse = ToDoRegelReferanse(),
        finnFaktum = BarnepensjonGrunnlag::grunnbeloep,
        finnFelt = { it }
    )

private val antallSoeskenIKullet: Regel<BarnepensjonGrunnlag, Int> =
    finnFaktumIGrunnlag(
        gjelderFra = BP_1967_DATO,
        beskrivelse = "Finner antall barn i kullet",
        regelReferanse = ToDoRegelReferanse(),
        finnFaktum = BarnepensjonGrunnlag::antallSoeskenIKullet,
        finnFelt = { it }
    )

val prosentsatsFoersteBarnKonstant = definerKonstant<BarnepensjonGrunnlag, BigDecimal>(
    gjelderFra = BP_1967_DATO,
    beskrivelse = "Prosentsats benyttet for første barn",
    regelReferanse = ToDoRegelReferanse(),
    verdi = 0.40.toBigDecimal()
)

private val belopForFoersteBarn = RegelMeta(
    gjelderFra = BP_1967_DATO,
    beskrivelse = "Satser i kr av for første barn",
    regelReferanse = ToDoRegelReferanse()
) multipliser (prosentsatsFoersteBarnKonstant og grunnbeloep)

val prosentsatsEtterfoelgendeBarnKonstant = definerKonstant<BarnepensjonGrunnlag, BigDecimal>(
    gjelderFra = BP_1967_DATO,
    beskrivelse = "Prosentsats benyttet for etterfølgende barn",
    regelReferanse = ToDoRegelReferanse(),
    verdi = 0.25.toBigDecimal()
)

private val belopForEtterfoelgendeBarn = RegelMeta(
    gjelderFra = BP_1967_DATO,
    beskrivelse = "Satser i kr av for etterfølgende barn",
    regelReferanse = ToDoRegelReferanse()
) multipliser (prosentsatsEtterfoelgendeBarnKonstant og grunnbeloep)

val barnekullRegel = RegelMeta(
    gjelderFra = BP_1967_DATO,
    beskrivelse = "Beregn uavkortet barnepensjon basert på størrelsen på barnekullet",
    regelReferanse = ToDoRegelReferanse()
) kombinerer belopForFoersteBarn og belopForEtterfoelgendeBarn og antallSoeskenIKullet med { foerstebarnSats, etterfoelgendeBarnSats, antallSoesken ->
    (foerstebarnSats + (etterfoelgendeBarnSats * antallSoesken.toBigDecimal())) / (antallSoesken + 1).toBigDecimal()
}