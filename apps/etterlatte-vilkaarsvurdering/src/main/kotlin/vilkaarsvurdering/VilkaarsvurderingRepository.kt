package no.nav.etterlatte.vilkaarsvurdering

interface VilkaarsvurderingRepository {
    fun lagre(vilkaarsvurdering: Vilkaarsvurdering): Vilkaarsvurdering
    fun oppdater(behandlingId: String, vurdertVilkaar: VurdertVilkaar): Vilkaarsvurdering
    fun hent(behandlingId: String): Vilkaarsvurdering?
}

class VilkaarsvurderingRepositoryInMemory(
    val db: MutableMap<String, Vilkaarsvurdering> = mutableMapOf()
) : VilkaarsvurderingRepository {

    override fun lagre(vilkaarsvurdering: Vilkaarsvurdering): Vilkaarsvurdering {
        db[vilkaarsvurdering.behandlingId] = vilkaarsvurdering
        return vilkaarsvurdering
    }

    override fun oppdater(behandlingId: String, vurdertVilkaar: VurdertVilkaar): Vilkaarsvurdering {
        hent(behandlingId)?.let { vilkaarsvurdering ->
            val oppdatertVilkaarsvurdering = vilkaarsvurdering.copy(
                vilkaar = vilkaarsvurdering.vilkaar.map {
                    if (it.type == vurdertVilkaar.vilkaarType) {
                        it.copy(vurdering = vurdertVilkaar.vurdertResultat)
                    } else {
                        it
                    }
                }
            )
            db[behandlingId] = oppdatertVilkaarsvurdering
            return oppdatertVilkaarsvurdering
        } ?: throw NullPointerException("Fant ingen vilkårsvurdering for behandlingId $behandlingId")
    }

    override fun hent(behandlingId: String): Vilkaarsvurdering? {
        return db[behandlingId]
    }
}