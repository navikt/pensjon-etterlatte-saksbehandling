package no.nav.etterlatte.vilkaarsvurdering.migrering

import no.nav.etterlatte.libs.common.vilkaarsvurdering.Utfall
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaar
import no.nav.etterlatte.libs.database.transaction
import no.nav.etterlatte.vilkaarsvurdering.DelvilkaarRepository
import javax.sql.DataSource

class MigreringRepository(
    private val delvilkaarRepository: DelvilkaarRepository,
    private val ds: DataSource
) {
    fun endreUtfallForAlleVilkaar(vilkaar: List<Vilkaar>, utfall: Utfall) =
        ds.transaction { tx ->
            vilkaar.forEach {
                delvilkaarRepository.settResultatPaaAlleDelvilkaar(
                    it.id,
                    tx,
                    utfall
                )
            }
        }
}