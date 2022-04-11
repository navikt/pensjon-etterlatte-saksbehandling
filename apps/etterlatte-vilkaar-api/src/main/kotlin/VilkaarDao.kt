package no.nav.etterlatte

import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.model.VilkaarResultatForBehandling
import java.sql.ResultSet
import java.util.*
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
                it.setObject(1, UUID.fromString(behandlingId))

                it.executeQuery().singleOrNull {
                    VilkaarResultatForBehandling(
                        behandling = getObject("behandling") as UUID,
                        avdoedSoeknad = getString("avdoedSoeknad")?.let { avdoedSoeknad ->
                            objectMapper.readTree(
                                avdoedSoeknad
                            )
                        },
                        soekerSoeknad = getString("soekerSoeknad")?.let { soekerSoeknad ->
                            objectMapper.readTree(
                                soekerSoeknad
                            )
                        },
                        soekerPdl = getString("avdoedPdl")?.let { soekerPdl -> objectMapper.readTree(soekerPdl) },
                        avdoedPdl = getString("avdoedPdl")?.let { avdoedPdl -> objectMapper.readTree(avdoedPdl) },
                        gjenlevendePdl = getString("avdoedPdl")?.let { gjenlevendePdl ->
                            objectMapper.readTree(
                                gjenlevendePdl
                            )
                        },
                        versjon = getLong("versjon"),
                        vilkaarResultat = objectMapper.readTree(getString("vilkaarResultat"))
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