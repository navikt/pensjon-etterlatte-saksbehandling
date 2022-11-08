package no.nav.etterlatte.vilkaarsvurdering

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaarsvurdering
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingResultat
import java.util.*

data class VilkaarsvurderingIntern(
    val behandlingId: UUID,
    val payload: JsonNode,
    val vilkaar: List<Vilkaar>,
    val virkningstidspunkt: Virkningstidspunkt,
    val resultat: VilkaarsvurderingResultat? = null
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

data class VilkaarsvurderingDto(
    val behandlingId: UUID,
    val vilkaar: List<Vilkaar>,
    val virkningstidspunkt: Virkningstidspunkt,
    val resultat: VilkaarsvurderingResultat? = null
)