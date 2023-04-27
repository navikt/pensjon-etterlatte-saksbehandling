package no.nav.etterlatte.vilkaarsvurdering.migrering

import kotliquery.sessionOf
import kotliquery.using
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Utfall
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaar
import no.nav.etterlatte.vilkaarsvurdering.DelvilkaarRepository
import javax.sql.DataSource

class MigreringRepository(
    private val delvilkaarRepository: DelvilkaarRepository,
    private val ds: DataSource
) {
    fun endreStatusForAlleVilkaar(vilkaar: List<Vilkaar>, utfall: Utfall) =
        using(sessionOf(ds)) { session ->
            session.transaction { tx ->
                vilkaar.forEach {
                    delvilkaarRepository.settResultatPaaAlleDelvilkaar(
                        it.id,
                        tx,
                        utfall
                    )
                }
            }
        }
}