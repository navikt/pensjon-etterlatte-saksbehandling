package no.nav.etterlatte.libs.common.vilkaarsvurdering

import java.time.YearMonth
import java.util.UUID

data class StatusOppdatertDto(
    val statusOppdatert: Boolean,
)

data class OppdaterVurdertVilkaar(
    val behandlingId: UUID,
    val vurdertVilkaar: VurdertVilkaar,
)

data class VilkaarsvurderingMedBehandlingGrunnlagsversjon(
    val vilkaarsvurdering: Vilkaarsvurdering,
    val behandlingGrunnlagVersjon: Long,
)

data class Vilkaarsvurdering(
    val id: UUID = UUID.randomUUID(),
    val behandlingId: UUID,
    val grunnlagVersjon: Long,
    val virkningstidspunkt: YearMonth,
    val vilkaar: List<Vilkaar>,
    val resultat: VilkaarsvurderingResultat? = null,
)

data class VilkaarTypeOgUtfall(
    val type: VilkaarType,
    val resultat: Utfall,
)

data class VurdertVilkaar(
    val vilkaarId: UUID,
    val hovedvilkaar: VilkaarTypeOgUtfall,
    val unntaksvilkaar: VilkaarTypeOgUtfall? = null,
    val vurdering: VilkaarVurderingData,
) {
    fun hovedvilkaarOgUnntaksvilkaarIkkeOppfylt() = hovedvilkaar.resultat == Utfall.IKKE_OPPFYLT && unntaksvilkaar == null
}
