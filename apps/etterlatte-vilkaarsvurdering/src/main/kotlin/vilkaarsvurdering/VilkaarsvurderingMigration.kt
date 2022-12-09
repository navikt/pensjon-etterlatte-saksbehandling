package no.nav.etterlatte.vilkaarsvurdering

class VilkaarsvurderingMigration(
    private val oldRepository: VilkaarsvurderingRepositoryImpl,
    private val repository2: VilkaarsvurderingRepository2
) {
    fun migrerVilkaarsvurdering() {
        oldRepository.hentAlle().forEach {
            repository2.opprettVilkaarsvurdering(it)
            it.resultat?.let { resultat ->
                repository2.lagreVilkaarsvurderingResultat(behandlingId = it.behandlingId, resultat = resultat)
            }
        }
    }
}