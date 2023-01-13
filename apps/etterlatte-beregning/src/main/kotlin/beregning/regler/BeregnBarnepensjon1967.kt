package no.nav.etterlatte.beregning.regler

import no.nav.etterlatte.beregning.regler.sats.barnepensjonSatsRegel
import no.nav.etterlatte.beregning.regler.trygdetidsfaktor.trygdetidsFaktor
import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.RegelMeta
import no.nav.etterlatte.libs.regler.RegelReferanse
import no.nav.etterlatte.libs.regler.benytter
import no.nav.etterlatte.libs.regler.med
import no.nav.etterlatte.libs.regler.og
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

val BP_1967_DATO: LocalDate = LocalDate.of(1967, 1, 1)

data class AvdoedForelder(val trygdetid: BigDecimal)
data class BarnepensjonGrunnlag(
    val grunnbeloep: FaktumNode<BigDecimal>,
    val antallSoeskenIKullet: FaktumNode<Int>,
    val avdoedForelder: FaktumNode<AvdoedForelder>
)

val beregnBarnepensjon1967Regel = RegelMeta(
    gjelderFra = BP_1967_DATO,
    beskrivelse = "Reduserer ytelsen mot opptjening i folketrygden",
    regelReferanse = RegelReferanse(id = "BP-BEREGNING-1967-REDUSERMOTTRYGDETID")
) benytter barnepensjonSatsRegel og trygdetidsFaktor med { sats, trygdetidsfaktor ->
    (sats * trygdetidsfaktor)
}

val kroneavrundetBarnepensjonRegel = RegelMeta(
    gjelderFra = BP_1967_DATO,
    beskrivelse = "Gjør en kroneavrunding av barnepensjonen",
    regelReferanse = RegelReferanse(id = "REGEL-KRONEAVRUNDING")
) benytter beregnBarnepensjon1967Regel med { beregnetBarnepensjon ->
    beregnetBarnepensjon.setScale(0, RoundingMode.HALF_UP).toInt()
}