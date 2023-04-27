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
    fun endreUtfallTilIkkeVurdertForAlleVilkaar(vilkaar: List<Vilkaar>) =
        ds.transaction { tx ->
            vilkaar.forEach {
                delvilkaarRepository.settResultatPaaAlleDelvilkaar(
                    it.id,
                    tx,
                    Utfall.IKKE_VURDERT
                )
            }
        }
}