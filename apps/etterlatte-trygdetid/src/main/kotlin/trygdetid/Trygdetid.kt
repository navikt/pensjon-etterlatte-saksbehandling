package no.nav.etterlatte.trygdetid

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.time.LocalDate
import java.time.Period
import java.util.*

data class Trygdetid(
    val id: UUID,
    val behandlingId: UUID,
    val trygdetidGrunnlag: List<TrygdetidGrunnlag>,
    val beregnetTrygdetid: BeregnetTrygdetid?,
    val opplysninger: List<Opplysningsgrunnlag>
)

data class BeregnetTrygdetid(
    val verdi: Int,
    val tidspunkt: Tidspunkt,
    val regelResultat: JsonNode
)

data class Opplysningsgrunnlag(
    val id: UUID,
    val type: Opplysningstype,
    val opplysning: JsonNode,
    val kilde: JsonNode
)

fun JsonNode.toLocalDate(): LocalDate = objectMapper.readValue(this.asText(), LocalDate::class.java)

data class TrygdetidGrunnlag(
    val id: UUID,
    val type: TrygdetidType,
    val bosted: String,
    val periode: TrygdetidPeriode,
    val kilde: String,
    val beregnetTrygdetid: BeregnetTrygdetidGrunnlag? = null
)

data class BeregnetTrygdetidGrunnlag(val verdi: Period, val tidspunkt: Tidspunkt, val regelResultat: JsonNode)

data class TrygdetidPeriode(
    val fra: LocalDate,
    val til: LocalDate
)

enum class TrygdetidType {
    NASJONAL,
    FREMTIDIG,
    UTLAND
}