import model.VilkaarResultatForBehandling


class VilkaarService(
    private val vilkaarDao: VilkaarDao
) {

    fun hentVilkaarResultat(behandlingId: String): VilkaarResultatForBehandling? =
        vilkaarDao.hentVilkaarResultat(behandlingId)
}