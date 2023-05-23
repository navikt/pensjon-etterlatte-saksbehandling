package no.nav.etterlatte.trygdetid

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
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
    val id: UUID?,
    val type: TrygdetidOpplysningType,
    val opplysning: JsonNode,
    val kilde: Grunnlagsopplysning.Kilde
) {
    companion object {
        fun ny(
            type: TrygdetidOpplysningType,
            kilde: Grunnlagsopplysning.Kilde?,
            verdi: LocalDate?
        ): Opplysningsgrunnlag =
            Opplysningsgrunnlag(
                id = null,
                type = type,
                opplysning = verdi?.toJsonNode() ?: throw Exception("Mangler opplysning"),
                kilde = kilde ?: throw Exception("Mangler kilde")
            )
    }
}

enum class TrygdetidOpplysningType {
    FOEDSELSDATO,
    DOEDSDATO,
    FYLT_16,
    FYLLER_66
}

data class TrygdetidGrunnlag(
    val id: UUID,
    val type: TrygdetidType,
    val bosted: String,
    val periode: TrygdetidPeriode,
    val kilde: Grunnlagsopplysning.Saksbehandler,
    val beregnetTrygdetid: BeregnetTrygdetidGrunnlag? = null
)

data class BeregnetTrygdetidGrunnlag(val verdi: Period, val tidspunkt: Tidspunkt, val regelResultat: JsonNode)

data class TrygdetidPeriode(
    val fra: LocalDate,
    val til: LocalDate
) {
    init {
        require(fra.isBefore(til) || fra.isEqual(til)) { "Ugyldig periode, fra må være før eller lik til" }
    }
}