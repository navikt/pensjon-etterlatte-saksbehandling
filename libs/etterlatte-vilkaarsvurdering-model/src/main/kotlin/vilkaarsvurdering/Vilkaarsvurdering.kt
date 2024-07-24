package no.nav.etterlatte.libs.common.vilkaarsvurdering

import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

data class VilkaarsvurderingDto(
    val behandlingId: UUID,
    val vilkaar: List<Vilkaar>,
    val virkningstidspunkt: YearMonth,
    val resultat: VilkaarsvurderingResultat? = null,
    val grunnlagVersjon: Long,
    val behandlingGrunnlagVersjon: Long?,
) {
    fun isYrkesskade() =
        this.vilkaar
            .filter {
                VilkaarType.yrkesskadeVilkaarTyper().contains(it.hovedvilkaar.type)
            }.any { it.hovedvilkaar.resultat == Utfall.OPPFYLT }

    fun isGrunnlagUtdatert(): Boolean =
        behandlingGrunnlagVersjon != null &&
            grunnlagVersjon < behandlingGrunnlagVersjon
}

data class Vilkaar(
    val hovedvilkaar: Delvilkaar,
    val unntaksvilkaar: List<Delvilkaar> = emptyList(),
    val vurdering: VilkaarVurderingData? = null,
    val id: UUID = UUID.randomUUID(),
)

fun List<Vilkaar>.kopier() = this.map { it.copy(id = UUID.randomUUID()) }

data class Delvilkaar(
    val type: VilkaarType,
    val tittel: String = type.tittel,
    val beskrivelse: String? = null,
    val spoersmaal: String? = null,
    val lovreferanse: Lovreferanse,
    val resultat: Utfall? = null,
) {
    constructor(
        type: VilkaarType,
        beskrivelse: String? = null,
        spoersmaal: String? = null,
        lovreferanse: Lovreferanse,
        resultat: Utfall? = null,
    ) : this(
        type = type,
        tittel = type.tittel,
        beskrivelse = beskrivelse,
        spoersmaal = spoersmaal,
        lovreferanse = lovreferanse,
        resultat = resultat,
    )
}

data class Lovreferanse(
    val paragraf: String,
    val ledd: Int? = null,
    val bokstav: String? = null,
    val lenke: String? = null,
)

data class VilkaarVurderingData(
    val kommentar: String?,
    val tidspunkt: LocalDateTime,
    val saksbehandler: String,
)

data class VilkaarsvurderingResultat(
    val utfall: VilkaarsvurderingUtfall,
    val kommentar: String?,
    val tidspunkt: LocalDateTime,
    val saksbehandler: String,
)

enum class Utfall {
    OPPFYLT,
    IKKE_OPPFYLT,
    IKKE_VURDERT,
}

enum class VilkaarsvurderingUtfall {
    OPPFYLT,
    IKKE_OPPFYLT,
}
