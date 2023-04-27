package no.nav.etterlatte.vilkaarsvurdering.migrering

import no.nav.etterlatte.libs.common.vilkaarsvurdering.Utfall
import no.nav.etterlatte.vilkaarsvurdering.VilkaarsvurderingRepository
import java.util.*

class MigreringService(
    private val migreringRepository: MigreringRepository,
    private val vilkaarsvurderingRepository: VilkaarsvurderingRepository
) {
    fun endreUtfallForAlleVilkaar(behandlingId: UUID, utfall: Utfall) =
        vilkaarsvurderingRepository.hent(behandlingId)
            ?.vilkaar
            ?.let { migreringRepository.endreUtfallForAlleVilkaar(it, utfall) }
            ?: throw IllegalStateException("Vilkårsvurdering mangler for behandling $behandlingId")
}