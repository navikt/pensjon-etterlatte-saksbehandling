package no.nav.etterlatte.vilkaarsvurdering

import no.nav.etterlatte.libs.common.grunnlag.Metadata
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaarsvurdering
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingResultat
import java.time.YearMonth
import java.util.*

data class VilkaarsvurderingIntern(
    val id: UUID = UUID.randomUUID(),
    val behandlingId: UUID,
    val vilkaar: List<Vilkaar>,
    val virkningstidspunkt: YearMonth,
    val resultat: VilkaarsvurderingResultat? = null,
    val grunnlagsmetadata: Metadata
) {
    fun toDomain() = Vilkaarsvurdering(
        behandlingId = this.behandlingId,
        vilkaar = this.vilkaar,
        virkningstidspunkt = this.virkningstidspunkt,
        resultat = requireNotNull(this.resultat) { "En vilkårsvurdering må ha et resultat" }
    )

    fun toDto() = VilkaarsvurderingDto(
        behandlingId = this.behandlingId,
        vilkaar = this.vilkaar,
        virkningstidspunkt = this.virkningstidspunkt,
        resultat = this.resultat
    )
}