package no.nav.etterlatte.beregning.regler

import no.nav.etterlatte.beregning.regler.sats.barnepensjonSatsRegel
import no.nav.etterlatte.beregning.regler.trygdetidsfaktor.trygdetidsFaktor
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.RegelMeta
import no.nav.etterlatte.libs.regler.RegelReferanse
import no.nav.etterlatte.libs.regler.benytter
import no.nav.etterlatte.libs.regler.med
import no.nav.etterlatte.libs.regler.og
import java.math.BigDecimal
import java.math.RoundingMode

data class AvdoedForelder(val trygdetid: BigDecimal)
data class BarnepensjonGrunnlag(
    val soeskenKull: FaktumNode<List<Foedselsnummer>>,
    val avdoedForelder: FaktumNode<AvdoedForelder>
)

val beregnBarnepensjon1967Regel = RegelMeta(
    gjelderFra = BP_1967_DATO,
    beskrivelse = "Reduserer ytelsen mot opptjening i folketrygden",
    regelReferanse = RegelReferanse(id = "BP-BEREGNING-1967-REDUSERMOTTRYGDETID")
) benytter barnepensjonSatsRegel og trygdetidsFaktor med { sats, trygdetidsfaktor ->
    sats.multiply(trygdetidsfaktor).setScale(DESIMALER_DELBEREGNING)
}

val kroneavrundetBarnepensjonRegel = RegelMeta(
    gjelderFra = BP_1967_DATO,
    beskrivelse = "Gjør en kroneavrunding av barnepensjonen",
    regelReferanse = RegelReferanse(id = "REGEL-KRONEAVRUNDING")
) benytter beregnBarnepensjon1967Regel med { beregnetBarnepensjon ->
    beregnetBarnepensjon.setScale(DESIMALER_RESULTAT, RoundingMode.HALF_UP).toInt()
}