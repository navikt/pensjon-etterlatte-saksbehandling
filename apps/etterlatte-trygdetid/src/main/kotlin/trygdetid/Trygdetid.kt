package no.nav.etterlatte.trygdetid

import java.time.LocalDate
import java.util.*

data class Trygdetid(
    val behandlingsId: UUID,
    val oppsummertTrygdetid: OppsummertTrygdetid?,
    val grunnlag: List<TrygdetidGrunnlag>
)

data class OppsummertTrygdetid(
    val nasjonalTrygdetid: Int,
    val fremtidigTrygdetid: Int,
    val totalt: Int
)

data class TrygdetidGrunnlag(
    val id: UUID,
    val type: TrygdetidType,
    val bosted: String,
    val periodeFra: LocalDate,
    val periodeTil: LocalDate,
    val kilde: String
)

enum class TrygdetidType {
    NASJONAL_TRYGDETID,
    FREMTIDIG_TRYGDETID,
    UTENLANDS_TRYGDETID
}