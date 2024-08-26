package vilkaarsvurdering

import no.nav.etterlatte.libs.common.vilkaarsvurdering.Utfall
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarVurderingData
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingResultat
import java.time.YearMonth
import java.util.UUID

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
