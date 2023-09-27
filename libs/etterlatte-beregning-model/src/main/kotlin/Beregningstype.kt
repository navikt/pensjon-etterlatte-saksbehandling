package no.nav.etterlatte.libs.common.beregning

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
