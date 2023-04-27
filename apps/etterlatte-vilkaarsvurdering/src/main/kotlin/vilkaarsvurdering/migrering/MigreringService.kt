package no.nav.etterlatte.vilkaarsvurdering.migrering

import no.nav.etterlatte.libs.common.vilkaarsvurdering.Utfall
import no.nav.etterlatte.vilkaarsvurdering.VilkaarsvurderingRepository
import java.util.*

class MigreringService(
    private val migreringRepository: MigreringRepository,
    private val vilkaarsvurderingRepository: VilkaarsvurderingRepository
) {
    fun endreStatusForAlleVilkaar(behandlingId: UUID, utfall: Utfall): Any {
        val vilkaarsvurdering = vilkaarsvurderingRepository.hent(behandlingId)
            ?: throw IllegalStateException("Vilkårsvurdering mangler for behandling $behandlingId")

        return migreringRepository.endreStatusForAlleVilkaar(vilkaarsvurdering.vilkaar, utfall)
    }
}