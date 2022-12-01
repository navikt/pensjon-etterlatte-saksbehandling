package no.nav.etterlatte.libs.common.vilkaarsvurdering

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

data class VilkaarsvurderingDto(
    val behandlingId: UUID,
    val vilkaar: List<VilkaarDto>,
    val virkningstidspunkt: VirkningstidspunktDto,
    val resultat: VilkaarsvurderingResultatDto?
)

data class VilkaarDto(
    val hovedvilkaar: HovedvilkaarDto,
    val unntaksvilkaar: List<UnntaksvilkaarDto>? = null,
    val vurdering: VilkaarVurderingDataDto? = null,
    val grunnlag: List<VilkaarsgrunnlagDto<out Any?>>? = null
)

data class EnkeltVilkaarDto(
    val type: String,
    val tittel: String,
    val beskrivelse: String? = null,
    val lovreferanse: LovreferanseDto,
    val resultat: UtfallDto? = null
)

typealias HovedvilkaarDto = EnkeltVilkaarDto
typealias UnntaksvilkaarDto = EnkeltVilkaarDto

data class LovreferanseDto(
    val paragraf: String,
    val ledd: Int? = null,
    val bokstav: String? = null,
    val lenke: String? = null
)

data class VilkaarVurderingDataDto(
    val kommentar: String?,
    val tidspunkt: LocalDateTime,
    val saksbehandler: String
)

enum class UtfallDto {
    OPPFYLT,
    IKKE_OPPFYLT,
    IKKE_VURDERT
}

data class VirkningstidspunktDto(
    val dato: YearMonth,
    val kilde: JsonNode
)

data class VilkaarsvurderingResultatDto(
    val utfall: VilkaarsvurderingUtfallDto,
    val kommentar: String?,
    val tidspunkt: LocalDateTime,
    val saksbehandler: String
)

enum class VilkaarsvurderingUtfallDto {
    OPPFYLT,
    IKKE_OPPFYLT
}

data class VilkaarsgrunnlagDto<T>(
    val id: UUID,
    val opplysningsType: VilkaarOpplysningTypeDto,
    val kilde: Grunnlagsopplysning.Kilde,
    val opplysning: T
)

enum class VilkaarOpplysningTypeDto {
    SOEKER_FOEDSELSDATO,
    AVDOED_DOEDSDATO,
    VIRKNINGSTIDSPUNKT
}

data class VurdertVilkaarDto(
    val hovedvilkaar: VilkaarTypeOgUtfallDto,
    val unntaksvilkaar: VilkaarTypeOgUtfallDto? = null,
    val kommentar: String?
)

data class VurdertVilkaarsvurderingResultatDto(
    val resultat: VilkaarsvurderingUtfallDto,
    val kommentar: String?
)

data class VilkaarTypeOgUtfallDto(
    val type: String,
    val resultat: UtfallDto
)