package no.nav.etterlatte

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.vikaar.Vilkaarsgrunnlag
import no.nav.etterlatte.libs.common.vikaar.VilkarIBehandling
import org.slf4j.LoggerFactory
import java.sql.ResultSet
import java.util.*
import javax.sql.DataSource

interface VilkaarDao {
    fun hentVilkaarResultat(behandlingId: String): VilkarIBehandling?
}

class VilkaarDaoJdbc(val dataSource: DataSource) : VilkaarDao {
    val logger = LoggerFactory.getLogger(VilkaarDaoJdbc::class.java)
    override fun hentVilkaarResultat(behandlingId: String): VilkarIBehandling? =
        dataSource.connection.use { connection ->

            val stmt =
                connection.prepareStatement("SELECT behandling, avdoedSoeknad, soekerSoeknad, soekerPdl, avdoedPdl, gjenlevendePdl, versjon, vilkaarResultat FROM vurdertvilkaar where behandling = ? ORDER BY versjon DESC LIMIT 1")

            stmt.use {
                it.setObject(1, UUID.fromString(behandlingId))
                it.executeQuery().singleOrNull {
                    logger.info("Fant vurdert vilkaar")
                    VilkarIBehandling(
                        behandling = getObject("behandling") as UUID,
                        grunnlag = Vilkaarsgrunnlag(
                            avdoedSoeknad = getString("avdoedSoeknad")?.let { objectMapper.readValue(it) },
                            soekerSoeknad = getString("soekerSoeknad")?.let { objectMapper.readValue(it) },
                            soekerPdl = getString("soekerPdl")?.let { objectMapper.readValue(it) },
                            avdoedPdl = getString("avdoedPdl")?.let { objectMapper.readValue(it) },
                            gjenlevendePdl = getString("gjenlevendePdl")?.let { objectMapper.readValue(it) }
                        ),
                        versjon = getLong("versjon"),
                        vilkaarResultat = objectMapper.readValue(getString("vilkaarResultat"))
                    )
                }
            }.also { logger.info("Returnerer fra hentVilkaarResultat  " + it.toString()) }
        }

    /*
    override fun hentVilkaarResultat(behandlingId: String): VurdertVilkaar? =
        dataSource.connection.use { connection ->
            val stmt = connection.prepareStatement(
                """SELECT behandling, avdoedSoeknad, soekerSoeknad, soekerPdl, avdoedPdl, gjenlevendePdl, versjon, vilkaarResultat
                        FROM vurdertvilkaar
                        WHERE behandling = ?
                        ORDER BY versjon DESC
                        LIMIT 1""".trimIndent()
            )

            stmt.use {
                it.setObject(1, UUID.fromString(behandlingId))

                it.executeQuery().singleOrNull {
                    VurdertVilkaar(
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
     */
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