package no.nav.etterlatte.trygdetid

data class Trygdetid(
    val grunnlag: List<TrygdetidGrunnlag>
)

data class TrygdetidGrunnlag(
    val bosted: String,
    val periodeFra: String,
    val periodeTil: String
)