package no.nav.etterlatte.vilkaarsvurdering.migrering

import no.nav.etterlatte.libs.common.vilkaarsvurdering.Utfall
import no.nav.etterlatte.vilkaarsvurdering.DelvilkaarRepository
import java.util.*

class MigreringRepository(val delvilkaarRepository: DelvilkaarRepository) {
    fun endreStatusForAlleVilkaar(behandlingId: UUID, utfall: Utfall): Any {
        return "" // TODO
    }
}