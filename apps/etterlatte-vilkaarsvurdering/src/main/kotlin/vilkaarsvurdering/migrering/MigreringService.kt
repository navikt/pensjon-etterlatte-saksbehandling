package no.nav.etterlatte.vilkaarsvurdering.migrering

import no.nav.etterlatte.libs.common.vilkaarsvurdering.Utfall
import java.util.*

class MigreringService(private val migreringRepository: MigreringRepository) {
    fun endreStatusForAlleVilkaar(behandlingId: UUID, utfall: Utfall) =
        migreringRepository.endreStatusForAlleVilkaar(behandlingId, utfall)
}