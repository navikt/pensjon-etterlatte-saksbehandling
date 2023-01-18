package no.nav.etterlatte.beregning.regler

import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.regler.FaktumNode
import java.math.BigDecimal

data class AvdoedForelder(val trygdetid: BigDecimal)
data class BarnepensjonGrunnlag(
    val soeskenKull: FaktumNode<List<Foedselsnummer>>,
    val avdoedForelder: FaktumNode<AvdoedForelder>
)