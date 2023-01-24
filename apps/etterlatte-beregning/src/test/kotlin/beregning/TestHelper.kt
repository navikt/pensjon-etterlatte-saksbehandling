package beregning.regler

import no.nav.etterlatte.beregning.regler.AvdoedForelder
import no.nav.etterlatte.beregning.regler.BarnepensjonGrunnlag
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.RegelPeriode
import no.nav.etterlatte.libs.testdata.grunnlag.kilde
import java.math.BigDecimal
import java.time.LocalDate

val REGEL_PERIODE = RegelPeriode(LocalDate.of(2023, 1, 1))

const val FNR_1 = "11057523044"
const val FNR_2 = "19040550081"
const val FNR_3 = "24014021406"

const val MAKS_TRYGDETID: Int = 40

fun barnepensjonGrunnlag(
    soeskenKull: List<String> = emptyList(),
    trygdeTid: BigDecimal = BigDecimal(MAKS_TRYGDETID)
) = BarnepensjonGrunnlag(
    soeskenKull = FaktumNode(soeskenKull.map { Foedselsnummer.of(it) }, kilde, "søskenkull"),
    avdoedForelder = FaktumNode(AvdoedForelder(trygdetid = trygdeTid), kilde, "trygdetid")
)