package no.nav.etterlatte.vilkaarsvurdering

import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingResultat
import java.time.YearMonth
import java.util.*

data class VilkaarsvurderingIntern(
    val id: UUID = UUID.randomUUID(),
    val sakId: Long,
    val behandlingId: UUID,
    val grunnlagVersjon: Long,
    val virkningstidspunkt: YearMonth,
    val vilkaar: List<Vilkaar>,
    val resultat: VilkaarsvurderingResultat? = null
) {
    fun toDto() = VilkaarsvurderingDto(
        behandlingId = this.behandlingId,
        virkningstidspunkt = this.virkningstidspunkt,
        vilkaar = this.vilkaar,
        resultat = this.resultat
    )
}