package no.nav.etterlatte.libs.common.trygdetid

import java.time.LocalDate
import java.util.*

data class TrygdetidDto(
    val id: UUID,
    val behandlingId: UUID,
    val beregnetTrygdetid: BeregnetTrygdetidDto?,
    val trygdetidGrunnlag: List<TrygdetidGrunnlagDto>
)

data class BeregnetTrygdetidDto(
    val nasjonal: Int,
    val fremtidig: Int,
    val total: Int
)

data class TrygdetidGrunnlagDto(
    val id: UUID?,
    val type: String,
    val bosted: String,
    val periodeFra: LocalDate,
    val periodeTil: LocalDate,
    val trygdetid: Int,
    val kilde: String
)