package no.nav.etterlatte.vilkaarsvurdering

interface VilkaarsvurderingRepository {
    fun lagre(vilkaarsvurdering: Vilkaarsvurdering): Vilkaarsvurdering
    fun oppdater(behandlingId: String, oppdatertVilkaar: Vilkaar): Vilkaarsvurdering
    fun hent(behandlingId: String): Vilkaarsvurdering?
}

class VilkaarsvurderingRepositoryInMemory(
    val db: MutableMap<String, Vilkaarsvurdering> = mutableMapOf()
) : VilkaarsvurderingRepository {

    override fun lagre(vilkaarsvurdering: Vilkaarsvurdering): Vilkaarsvurdering {
        db[vilkaarsvurdering.behandlingId] = vilkaarsvurdering
        return vilkaarsvurdering
    }

    override fun oppdater(behandlingId: String, oppdatertVilkaar: Vilkaar): Vilkaarsvurdering {
        val vilkaarsvurdering = db[behandlingId]
        if (vilkaarsvurdering != null) {
            val oppdaterteVilkaar = vilkaarsvurdering.vilkaar.map {
                if (it.type == oppdatertVilkaar.type) {
                    oppdatertVilkaar
                } else {
                    it
                }
            }
            val oppdatertVilkaarsvurdering = vilkaarsvurdering.copy(vilkaar = oppdaterteVilkaar)
            db[behandlingId] = oppdatertVilkaarsvurdering
            return oppdatertVilkaarsvurdering
        } else {
            throw NullPointerException("Fant ingen vilkårsvurdering for behandlingId $behandlingId")
        }
    }

    override fun hent(behandlingId: String): Vilkaarsvurdering? {
        return db[behandlingId]
    }
}