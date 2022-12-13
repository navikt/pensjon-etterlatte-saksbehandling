package no.nav.etterlatte.libs.common.vilkaarsvurdering

import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.time.YearMonth
import java.util.*

data class VilkaarsvurderingDto(
    val behandlingId: UUID,
    val vilkaar: List<Vilkaar>,
    val virkningstidspunkt: YearMonth,
    val resultat: VilkaarsvurderingResultat? = null
)

data class Vilkaarsvurdering(
    val behandlingId: UUID,
    val vilkaar: List<Vilkaar>,
    val virkningstidspunkt: YearMonth,
    val resultat: VilkaarsvurderingResultat
)

data class Vilkaar(
    val hovedvilkaar: Hovedvilkaar,
    val unntaksvilkaar: List<Unntaksvilkaar>? = null,
    val vurdering: VilkaarVurderingData? = null,
    val grunnlag: List<Vilkaarsgrunnlag<out Any?>>? = null,
    val id: UUID? = null
)

data class Delvilkaar(
    val type: VilkaarType,
    val tittel: String,
    val beskrivelse: String? = null,
    val lovreferanse: Lovreferanse,
    val resultat: Utfall? = null
)

typealias Hovedvilkaar = Delvilkaar
typealias Unntaksvilkaar = Delvilkaar

data class Lovreferanse(
    val paragraf: String,
    val ledd: Int? = null,
    val bokstav: String? = null,
    val lenke: String? = null
)

data class VilkaarVurderingData(
    val kommentar: String?,
    val tidspunkt: Tidspunkt,
    val saksbehandler: String
)

data class VilkaarsvurderingResultat(
    val utfall: VilkaarsvurderingUtfall,
    val kommentar: String?,
    val tidspunkt: Tidspunkt,
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
    val vilkaarId: UUID,
    val hovedvilkaar: VilkaarTypeOgUtfall,
    val unntaksvilkaar: VilkaarTypeOgUtfall? = null,
    val vurdering: VilkaarVurderingData
) {
    fun hovedvilkaarOgUnntaksvilkaarIkkeOppfylt() =
        hovedvilkaar.resultat == Utfall.IKKE_OPPFYLT && unntaksvilkaar == null
}

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