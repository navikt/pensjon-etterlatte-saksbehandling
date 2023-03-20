package no.nav.etterlatte.trygdetid

import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.time.LocalDate
import java.util.*

data class Trygdetid(
    val id: UUID,
    val behandlingId: UUID,
    val opprettet: Tidspunkt,
    val trygdetidGrunnlag: List<TrygdetidGrunnlag>,
    val beregnetTrygdetid: BeregnetTrygdetid?
)

data class BeregnetTrygdetid(
    val nasjonal: Int,
    val fremtidig: Int,
    val total: Int
)

data class TrygdetidGrunnlag(
    val id: UUID,
    val type: TrygdetidType,
    val bosted: String,
    val periode: TrygdetidPeriode,
    val kilde: String
)

data class TrygdetidPeriode(
    val fra: LocalDate,
    val til: LocalDate
)

enum class TrygdetidType {
    NASJONAL,
    FREMTIDIG,
    UTLAND
}