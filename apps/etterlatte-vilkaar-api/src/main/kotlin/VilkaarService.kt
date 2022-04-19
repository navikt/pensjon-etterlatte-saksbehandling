package no.nav.etterlatte

import no.nav.etterlatte.libs.common.vikaar.VilkarIBehandling


class VilkaarService(
    private val vilkaarDao: VilkaarDao
) {

    fun hentVilkaarResultat(behandlingId: String): VilkarIBehandling? =
        vilkaarDao.hentVilkaarResultat(behandlingId)
}