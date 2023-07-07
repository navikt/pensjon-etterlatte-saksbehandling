package no.nav.etterlatte.libs.common.vilkaarsvurdering

import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

data class VilkaarsvurderingDto(
    val behandlingId: UUID,
    val vilkaar: List<Vilkaar>,
    val virkningstidspunkt: YearMonth,
    val resultat: VilkaarsvurderingResultat? = null
) {
    fun isYrkesskade() = this.vilkaar.filter {
        VilkaarType.yrkesskadeVilkaarTyper().contains(it.hovedvilkaar.type)
    }.any { it.hovedvilkaar.resultat == Utfall.OPPFYLT }
}

data class Vilkaar(
    val hovedvilkaar: Delvilkaar,
    val unntaksvilkaar: List<Delvilkaar> = emptyList(),
    val vurdering: VilkaarVurderingData? = null,
    val grunnlag: List<Vilkaarsgrunnlag<out Any?>> = emptyList(),
    val kopiert: Boolean = false,
    val id: UUID = UUID.randomUUID()
)

fun List<Vilkaar>.kopier() = this.map {
    it.copy(
        id = UUID.randomUUID(),
        kopiert = true
    )
}

data class Delvilkaar(
    val type: VilkaarType,
    val tittel: String,
    val beskrivelse: String? = null,
    val spoersmaal: String? = null,
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

data class Vilkaarsgrunnlag<T>(
    val id: UUID,
    val opplysningsType: VilkaarOpplysningType,
    val kilde: Grunnlagsopplysning.Kilde,
    val opplysning: T
)

enum class VilkaarOpplysningType {
    SOEKNAD_MOTTATT_DATO,
    SOEKER_FOEDSELSDATO,
    AVDOED_DOEDSDATO,
    VIRKNINGSTIDSPUNKT
}