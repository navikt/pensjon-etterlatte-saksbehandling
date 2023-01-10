package no.nav.etterlatte.libs.regler.beregning

import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.RegelMeta
import no.nav.etterlatte.libs.regler.RegelReferanse
import no.nav.etterlatte.libs.regler.beregning.barnepensjon1967.BP_1967_DATO
import no.nav.etterlatte.libs.regler.beregning.barnepensjon1967.beregnBarnepensjon1967Regel
import no.nav.etterlatte.libs.regler.beregning.barnepensjon2024.beregnBarnepensjon2024Regel
import no.nav.etterlatte.libs.regler.og
import no.nav.etterlatte.libs.regler.velgNyesteGyldige
import java.math.BigDecimal

val toDoRegelReferanse = RegelReferanse("ToDo")

data class AvdoedForelder(val trygdetid: BigDecimal)
data class BarnepensjonGrunnlag(
    val grunnbeloep: FaktumNode<BigDecimal>,
    val antallSoeskenIKullet: FaktumNode<Int>,
    val avdoedForelder: FaktumNode<AvdoedForelder>
)

val beregnBarnepensjonRegel = RegelMeta(
    gjelderFra = BP_1967_DATO,
    beskrivelse = "Velger hvilke regelverk som skal anvendes for beregning av barnepensjon",
    regelReferanse = toDoRegelReferanse
) velgNyesteGyldige (beregnBarnepensjon1967Regel og beregnBarnepensjon2024Regel)