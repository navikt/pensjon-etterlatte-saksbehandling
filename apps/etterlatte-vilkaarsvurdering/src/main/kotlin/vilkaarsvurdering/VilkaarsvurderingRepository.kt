package no.nav.etterlatte.vilkaarsvurdering

import java.util.*

interface VilkaarsvurderingRepository {
    fun hent(behandlingId: UUID): VilkaarsvurderingDao?
    fun lagre(vilkaarsvurdering: VilkaarsvurderingDao): VilkaarsvurderingDao
    fun oppdater(vilkaarsvurdering: VilkaarsvurderingDao): VilkaarsvurderingDao
}

class VilkaarsvurderingRepositoryInMemory(
    private val db: MutableMap<UUID, VilkaarsvurderingDao> = mutableMapOf()
) : VilkaarsvurderingRepository {

    override fun hent(behandlingId: UUID): VilkaarsvurderingDao? {
        return db[behandlingId]
    }

    override fun lagre(vilkaarsvurdering: VilkaarsvurderingDao): VilkaarsvurderingDao {
        db[vilkaarsvurdering.behandlingId] = vilkaarsvurdering
        return vilkaarsvurdering
    }

    override fun oppdater(vilkaarsvurdering: VilkaarsvurderingDao): VilkaarsvurderingDao {
        return lagre(vilkaarsvurdering)
    }
}