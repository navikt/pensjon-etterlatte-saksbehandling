package no.nav.etterlatte.vilkaarsvurdering

interface VilkaarsvurderingRepository {
    fun hent(behandlingId: String): Vilkaarsvurdering?
    fun lagre(vilkaarsvurdering: Vilkaarsvurdering): Vilkaarsvurdering
    fun oppdaterVurderingPaaVilkaar(behandlingId: String, vurdertVilkaar: VurdertVilkaar): Vilkaarsvurdering
    fun slettVurderingPaaVilkaar(behandlingId: String, vilkaarType: VilkaarType): Vilkaarsvurdering
}

class VilkaarsvurderingRepositoryInMemory(
    val db: MutableMap<String, Vilkaarsvurdering> = mutableMapOf<String, Vilkaarsvurdering>().apply {
        put(
            "1",
            Vilkaarsvurdering("1", "json", vilkaarBarnepensjon())
        )
    }
) : VilkaarsvurderingRepository {

    override fun hent(behandlingId: String): Vilkaarsvurdering? {
        return db[behandlingId]
    }

    override fun lagre(vilkaarsvurdering: Vilkaarsvurdering): Vilkaarsvurdering {
        db[vilkaarsvurdering.behandlingId] = vilkaarsvurdering
        return vilkaarsvurdering
    }

    override fun oppdaterVurderingPaaVilkaar(behandlingId: String, vurdertVilkaar: VurdertVilkaar): Vilkaarsvurdering {
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

    override fun slettVurderingPaaVilkaar(behandlingId: String, vilkaarType: VilkaarType): Vilkaarsvurdering {
        hent(behandlingId)?.let { vilkaarsvurdering ->
            val oppdatertVilkaarsvurdering = vilkaarsvurdering.copy(
                vilkaar = vilkaarsvurdering.vilkaar.map {
                    if (it.type == vilkaarType) {
                        it.copy(vurdering = null)
                    } else {
                        it
                    }
                }
            )
            db[behandlingId] = oppdatertVilkaarsvurdering
            return oppdatertVilkaarsvurdering
        } ?: throw NullPointerException("Fant ingen vilkårsvurdering for behandlingId $behandlingId")
    }
}