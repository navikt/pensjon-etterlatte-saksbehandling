package beregning

import beregning.barnepensjon1967.BP_1967_DATO
import beregning.barnepensjon1967.beregnBarnepensjon1967Regel
import beregning.barnepensjon2024.beregnBarnepensjon2024Regel
import regler.FaktumNode
import regler.RegelGrunnlag
import regler.RegelMeta
import regler.RegelPeriode
import regler.ToDoRegelReferanse
import regler.og
import regler.velgNyesteGyldige
import java.math.BigDecimal

data class AvdoedForelder(val trygdetid: BigDecimal)
data class BarnepensjonGrunnlag(
    override val periode: FaktumNode<RegelPeriode>,
    val grunnbeloep: FaktumNode<BigDecimal>,
    val antallSoeskenIKullet: FaktumNode<Int>,
    val avdoedForelder: FaktumNode<AvdoedForelder>
) : RegelGrunnlag

val beregnBarnepensjonRegel = RegelMeta(
    gjelderFra = BP_1967_DATO,
    beskrivelse = "Velger hvilke regelverk som skal anvendes for beregning av barnepensjon",
    regelReferanse = ToDoRegelReferanse()
) velgNyesteGyldige (beregnBarnepensjon1967Regel og beregnBarnepensjon2024Regel)