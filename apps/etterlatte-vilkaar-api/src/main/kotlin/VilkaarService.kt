package no.nav.etterlatte

import no.nav.etterlatte.model.VurdertVilkaar


class VilkaarService(
    private val vilkaarDao: VilkaarDao
) {

    fun hentVilkaarResultat(behandlingId: String): VurdertVilkaar? =
        vilkaarDao.hentVilkaarResultat(behandlingId)
}