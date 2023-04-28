package no.nav.etterlatte.vilkaarsvurdering.migrering

import no.nav.etterlatte.vilkaarsvurdering.VilkaarsvurderingRepository
import java.util.*

class MigreringService(
    private val migreringRepository: MigreringRepository,
    private val vilkaarsvurderingRepository: VilkaarsvurderingRepository
) {
    fun endreUtfallTilIkkeVurdertForAlleVilkaar(behandlingId: UUID) =
        vilkaarsvurderingRepository.hent(behandlingId)
            ?.vilkaar
            ?.let { migreringRepository.endreUtfallTilIkkeVurdertForAlleVilkaar(it) }
            ?: throw IllegalStateException("Vilkårsvurdering mangler for behandling $behandlingId")
}