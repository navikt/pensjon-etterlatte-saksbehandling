package no.nav.etterlatte.trygdetid

import java.util.*

data class Trygdetid(
    val behandlingsId: UUID,
    val oppsummertTrygdetid: Int?,
    val grunnlag: List<TrygdetidGrunnlag>
)

data class TrygdetidGrunnlag(
    val bosted: String,
    val periodeFra: String,
    val periodeTil: String
)