package no.nav.etterlatte.libs.regler.beregning

import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.RegelMeta
import no.nav.etterlatte.libs.regler.RegelReferanse
import no.nav.etterlatte.libs.regler.benytter
import no.nav.etterlatte.libs.regler.beregning.barnepensjon1967.BP_1967_DATO
import no.nav.etterlatte.libs.regler.beregning.barnepensjon1967.beregnBarnepensjon1967Regel
import no.nav.etterlatte.libs.regler.beregning.barnepensjon2024.beregnBarnepensjon2024Regel
import no.nav.etterlatte.libs.regler.med
import no.nav.etterlatte.libs.regler.og
import no.nav.etterlatte.libs.regler.velgNyesteGyldige
import java.math.BigDecimal
import java.math.RoundingMode

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

val kroneavrundetBarnepensjonRegel = RegelMeta(
    gjelderFra = BP_1967_DATO,
    beskrivelse = "Gjør en kroneavrunding av barnepensjonen",
    regelReferanse = RegelReferanse(id = "REGEL-KRONEAVRUNDING")
) benytter beregnBarnepensjonRegel med { beregnetBarnepensjon ->
    beregnetBarnepensjon.setScale(0, RoundingMode.HALF_UP).toInt()
}