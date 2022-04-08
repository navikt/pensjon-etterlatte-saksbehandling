import no.nav.etterlatte.libs.common.vikaar.VilkaarResultat
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import model.VilkaarResultatForBehandling
import java.time.LocalDateTime

interface VilkaarDao {
    fun hentVilkaarResultat(behandlingId: String): VilkaarResultatForBehandling?
}

class VilkarDaoInMemory() : VilkaarDao {
    val vilkaar = HashMap<String, VilkaarResultatForBehandling>()

    init {
        vilkaar.put(
            "1234", VilkaarResultatForBehandling(
                behandlingId = "1234",
                VilkaarResultat(
                    VurderingsResultat.OPPFYLT,
                    emptyList(),
                    LocalDateTime.now()
                )
            )
        )
    }

    override fun hentVilkaarResultat(behandlingId: String): VilkaarResultatForBehandling? = vilkaar.get(behandlingId)
}
