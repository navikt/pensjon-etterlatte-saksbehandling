package no.nav.etterlatte.libs.common.beregning

import no.nav.etterlatte.libs.common.IntBroek
import no.nav.etterlatte.regler.Beregningstall

// TODO kunne man like gjerne brukt sakType?
enum class Beregningstype {
    BP,
    OMS,
}

enum class BeregningsMetode {
    BEST,
    NASJONAL,
    PRORATA,
}

data class BeregningsMetodeBeregningsgrunnlag(
    val beregningsMetode: BeregningsMetode,
    val begrunnelse: String? = null,
)

data class SamletTrygdetidMedBeregningsMetode(
    val beregningsMetode: BeregningsMetode,
    val samletTrygdetidNorge: Beregningstall?,
    val samletTrygdetidTeoretisk: Beregningstall?,
    val prorataBroek: IntBroek?,
    val ident: String,
) {
    fun broek() = Beregningstall(this.prorataBroek?.let { it.teller.toDouble() / it.nevner.toDouble() } ?: 1.0)
}

// Må kun inneholde felter som er felles for BP og OMS
data class BeregningsGrunnlagFellesDto(
    val beregningsMetode: BeregningsMetodeBeregningsgrunnlag,
)

data class BeregningsmetodeForAvdoed(
    val avdoed: String,
    val beregningsMetode: BeregningsMetodeBeregningsgrunnlag,
)
