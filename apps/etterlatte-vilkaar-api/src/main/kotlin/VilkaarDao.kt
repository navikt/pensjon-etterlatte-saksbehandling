import model.VilkaarResultatForBehandling
import no.nav.etterlatte.libs.common.vikaar.VilkaarResultat
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import java.time.LocalDateTime
import javax.sql.DataSource

interface VilkaarDao {
    fun hentVilkaarResultat(behandlingId: String): VilkaarResultatForBehandling?
}

class VilkaarDaoInMemory() : VilkaarDao {
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

class VilkaarDaoJdbc(val dataSource: DataSource) : VilkaarDao {
    override fun hentVilkaarResultat(behandlingId: String): VilkaarResultatForBehandling? {
        dataSource.connection.use { connection ->
            val stmt = connection.prepareStatement(

            )
        }

    }

}