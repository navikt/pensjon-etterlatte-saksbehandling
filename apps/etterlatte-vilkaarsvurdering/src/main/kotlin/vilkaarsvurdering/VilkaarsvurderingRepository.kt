package no.nav.etterlatte.vilkaarsvurdering

import java.util.*

interface VilkaarsvurderingRepository {
    fun hent(behandlingId: UUID): VilkaarsvurderingIntern?
    fun lagre(vilkaarsvurdering: VilkaarsvurderingIntern): VilkaarsvurderingIntern
    fun oppdater(vilkaarsvurdering: VilkaarsvurderingIntern): VilkaarsvurderingIntern
}

class VilkaarsvurderingRepositoryInMemory(
    private val db: MutableMap<UUID, VilkaarsvurderingIntern> = mutableMapOf()
) : VilkaarsvurderingRepository {

    override fun hent(behandlingId: UUID): VilkaarsvurderingIntern? {
        return db[behandlingId]
    }

    override fun lagre(vilkaarsvurdering: VilkaarsvurderingIntern): VilkaarsvurderingIntern {
        db[vilkaarsvurdering.behandlingId] = vilkaarsvurdering
        return vilkaarsvurdering
    }

    override fun oppdater(vilkaarsvurdering: VilkaarsvurderingIntern): VilkaarsvurderingIntern {
        return lagre(vilkaarsvurdering)
    }
}