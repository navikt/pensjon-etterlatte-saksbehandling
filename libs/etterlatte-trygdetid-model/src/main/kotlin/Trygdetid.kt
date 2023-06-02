package no.nav.etterlatte.libs.common.trygdetid

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.time.LocalDate
import java.util.*

data class TrygdetidDto(
    val id: UUID,
    val behandlingId: UUID,
    val beregnetTrygdetid: BeregnetTrygdetidDto?,
    val trygdetidGrunnlag: List<TrygdetidGrunnlagDto>,
    val opplysninger: GrunnlagOpplysningerDto
)

data class GrunnlagOpplysningerDto(
    val avdoedDoedsdato: OpplysningsgrunnlagDto?,
    val avdoedFoedselsdato: OpplysningsgrunnlagDto?,
    val avdoedFylteSeksten: OpplysningsgrunnlagDto?,
    val avdoedFyllerSeksti: OpplysningsgrunnlagDto?
)

data class OpplysningsgrunnlagDto(
    val opplysning: JsonNode,
    val kilde: OpplysningkildeDto
)

data class OpplysningkildeDto(
    val type: String,
    val tidspunkt: String
)

data class BeregnetTrygdetidDto(
    val total: Int,
    val tidspunkt: Tidspunkt
)

data class TrygdetidGrunnlagDto(
    val id: UUID?,
    val type: String,
    val bosted: String,
    val periodeFra: LocalDate,
    val periodeTil: LocalDate,
    val kilde: TrygdetidGrunnlagKildeDto?,
    val beregnet: BeregnetTrygdetidGrunnlagDto?,
    val begrunnelse: String?
)

data class TrygdetidGrunnlagKildeDto(
    val tidspunkt: String,
    val ident: String
)

data class BeregnetTrygdetidGrunnlagDto(
    val dager: Int,
    val maaneder: Int,
    val aar: Int
)