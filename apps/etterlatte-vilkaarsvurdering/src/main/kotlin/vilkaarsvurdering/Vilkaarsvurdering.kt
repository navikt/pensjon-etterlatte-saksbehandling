package no.nav.etterlatte.vilkaarsvurdering.vilkaarsvurdering

import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.Metadata
import no.nav.etterlatte.vilkaarsvurdering.vilkaar.VilkaarType
import java.time.LocalDateTime
import java.util.*

data class Vilkaarsvurdering(
    val behandlingId: UUID,
    val vilkaar: List<Vilkaar>,
    val virkningstidspunkt: Virkningstidspunkt,
    val resultat: VilkaarsvurderingResultat? = null,
    val grunnlagsmetadata: Metadata
)

data class Vilkaar(
    val hovedvilkaar: Hovedvilkaar,
    val unntaksvilkaar: List<Unntaksvilkaar>? = null,
    val vurdering: VilkaarVurderingData? = null,
    val grunnlag: List<Vilkaarsgrunnlag<out Any?>>? = null
)

data class Hovedvilkaar(
    val type: VilkaarType,
    val tittel: String,
    val beskrivelse: String? = null,
    val lovreferanse: Lovreferanse,
    val resultat: Utfall? = null
)

data class Unntaksvilkaar(
    val type: VilkaarType,
    val tittel: String,
    val beskrivelse: String? = null,
    val lovreferanse: Lovreferanse,
    val resultat: Utfall? = null
)

data class Lovreferanse(
    val paragraf: String,
    val ledd: Int? = null,
    val bokstav: String? = null,
    val lenke: String? = null
)

data class VilkaarVurderingData(
    val kommentar: String?,
    val tidspunkt: LocalDateTime,
    val saksbehandler: String
)

data class VilkaarsvurderingResultat(
    val utfall: VilkaarsvurderingUtfall,
    val kommentar: String?,
    val tidspunkt: LocalDateTime,
    val saksbehandler: String
)

enum class Utfall {
    OPPFYLT,
    IKKE_OPPFYLT,
    IKKE_VURDERT
}

enum class VilkaarsvurderingUtfall {
    OPPFYLT,
    IKKE_OPPFYLT
}

data class VilkaarTypeOgUtfall(
    val type: VilkaarType,
    val resultat: Utfall
)

data class VurdertVilkaar(
    val hovedvilkaar: VilkaarTypeOgUtfall,
    val unntaksvilkaar: VilkaarTypeOgUtfall? = null,
    val vurdering: VilkaarVurderingData
)

data class Vilkaarsgrunnlag<T>(
    val id: UUID,
    val opplysningsType: VilkaarOpplysningType,
    val kilde: Grunnlagsopplysning.Kilde,
    val opplysning: T
)

enum class VilkaarOpplysningType {
    SOEKER_FOEDSELSDATO,
    AVDOED_DOEDSDATO,
    VIRKNINGSTIDSPUNKT
}