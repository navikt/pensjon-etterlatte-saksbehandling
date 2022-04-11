package no.nav.etterlatte

import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.model.VilkaarResultatForBehandling
import java.sql.ResultSet
import javax.sql.DataSource

interface VilkaarDao {
    fun hentVilkaarResultat(behandlingId: String): VilkaarResultatForBehandling?
}

class VilkaarDaoInMemory() : VilkaarDao {
    val vilkaar = HashMap<String, VilkaarResultatForBehandling>()

    /*
    init {
        vilkaar.put(
            "1234", VilkaarResultatForBehandling(
                behandling = "1234",
                VilkaarResultat(
                    VurderingsResultat.OPPFYLT,
                    emptyList(),
                    LocalDateTime.now()
                )
            )
        )
    }

     */

    override fun hentVilkaarResultat(behandlingId: String): VilkaarResultatForBehandling? = vilkaar.get(behandlingId)
}

class VilkaarDaoJdbc(val dataSource: DataSource) : VilkaarDao {
    override fun hentVilkaarResultat(behandlingId: String): VilkaarResultatForBehandling? =
        dataSource.connection.use { connection ->
            val stmt = connection.prepareStatement(
                "SELECT behandling, avdoedSoeknad, soekerSoeknad, soekerPdl, avdoedPdl, gjenlevendePdl, versjon, vilkaarResultat " +
                        "FROM vurdertvilkaar " +
                        "WHERE behandling = ?"
            )

            stmt.use {
                it.setString(1, behandlingId)

                it.executeQuery().singleOrNull {
                    VilkaarResultatForBehandling(
                        behandling = getString("behandling"),
                        avdoedSoeknad = getString("avdoedSoeknad").let { avdoedSoeknad ->
                            objectMapper.valueToTree(
                                avdoedSoeknad
                            )
                        },
                        soekerSoeknad = getString("soekerSoeknad").let { soekerSoeknad ->
                            objectMapper.valueToTree(
                                soekerSoeknad
                            )
                        },
                        soekerPdl = getString("avdoedPdl").let { soekerPdl -> objectMapper.valueToTree(soekerPdl) },
                        avdoedPdl = getString("avdoedPdl").let { avdoedPdl -> objectMapper.valueToTree(avdoedPdl) },
                        gjenlevendePdl = getString("avdoedPdl").let { gjenlevendePdl ->
                            objectMapper.valueToTree(
                                gjenlevendePdl
                            )
                        },
                        versjon = getLong("versjon"),
                        vilkaarResultat = getString("vilkaarResultat").let { vilkaarResultat ->
                            objectMapper.valueToTree(
                                vilkaarResultat
                            )
                        }
                    )
                }
            }
        }

    private fun <T> ResultSet.singleOrNull(block: ResultSet.() -> T): T? {
        return if (next()) {
            block().also {
                require(!next()) { "Skal være unik" }
            }
        } else {
            null
        }
    }
}