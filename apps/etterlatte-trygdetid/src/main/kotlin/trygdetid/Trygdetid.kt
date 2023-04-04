package no.nav.etterlatte.trygdetid

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.objectMapper
import java.time.LocalDate
import java.util.*

data class Trygdetid(
    val id: UUID,
    val behandlingId: UUID,
    val trygdetidGrunnlag: List<TrygdetidGrunnlag>,
    val beregnetTrygdetid: BeregnetTrygdetid?,
    val opplysninger: List<Opplysningsgrunnlag>
)

data class BeregnetTrygdetid(
    val nasjonal: Int,
    val fremtidig: Int,
    val total: Int
)

data class Opplysningsgrunnlag(
    val id: UUID,
    val type: Opplysningstype,
    val opplysning: JsonNode
)

fun JsonNode.toLocalDate(): LocalDate = objectMapper.readValue(this.asText(), LocalDate::class.java)

data class TrygdetidGrunnlag(
    val id: UUID,
    val type: TrygdetidType,
    val bosted: String,
    val periode: TrygdetidPeriode,
    val trygdetid: Int,
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