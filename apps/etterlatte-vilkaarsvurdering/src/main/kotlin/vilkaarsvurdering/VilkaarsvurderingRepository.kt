package no.nav.etterlatte.vilkaarsvurdering

import java.util.UUID

interface VilkaarsvurderingRepository {
    fun hent(behandlingId: UUID): Vilkaarsvurdering?
    fun lagre(vilkaarsvurdering: Vilkaarsvurdering): Vilkaarsvurdering
    fun oppdater(vilkaarsvurdering: Vilkaarsvurdering): Vilkaarsvurdering
}

class VilkaarsvurderingRepositoryInMemory(
    private val db: MutableMap<UUID, Vilkaarsvurdering> = mutableMapOf()
) : VilkaarsvurderingRepository {

    override fun hent(behandlingId: UUID): Vilkaarsvurdering? {
        return db[behandlingId]
    }

    override fun lagre(vilkaarsvurdering: Vilkaarsvurdering): Vilkaarsvurdering {
        db[vilkaarsvurdering.behandlingId] = vilkaarsvurdering
        return vilkaarsvurdering
    }

    override fun oppdater(vilkaarsvurdering: Vilkaarsvurdering): Vilkaarsvurdering {
        return lagre(vilkaarsvurdering)
    }
}