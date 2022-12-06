package beregning

import FaktumNode
import RegelGrunnlag
import ToDoRegelReferanse
import beregning.barnepensjon1967.BP_1967_DATO
import beregning.barnepensjon1967.beregnBarnepensjon1967Regel
import beregning.barnepensjon2024.beregnBarnepensjon2024Regel
import regler.RegelMeta
import regler.og
import regler.velgNyesteGyldige
import java.math.BigDecimal
import java.time.YearMonth

data class AvdoedForelder(val trygdetid: BigDecimal)
data class BarnepensjonGrunnlag(
    override val virkningstidspunkt: FaktumNode<YearMonth>,
    val grunnbeloep: FaktumNode<BigDecimal>,
    val antallSoeskenIKullet: FaktumNode<Int>,
    val avdoedForelder: FaktumNode<AvdoedForelder>
) : RegelGrunnlag

val beregnBarnepensjonRegel = RegelMeta(
    gjelderFra = BP_1967_DATO,
    beskrivelse = "Velger hvilke regelverk som skal anvendes for beregning av barnepensjon",
    regelReferanse = ToDoRegelReferanse()
) velgNyesteGyldige (beregnBarnepensjon1967Regel og beregnBarnepensjon2024Regel)