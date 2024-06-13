package no.nav.etterlatte.statistikk.database

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.database.toList
import no.nav.etterlatte.statistikk.domain.Beregning
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.time.YearMonth
import java.util.UUID
import javax.sql.DataSource

class OppdaterBeregningDao(
    private val datasource: DataSource,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    companion object {
        fun using(datasource: DataSource): OppdaterBeregningDao = OppdaterBeregningDao(datasource)
    }

    fun hentBehandlingerUtenOppdatertBeregning(limit: Int): List<UUID> =
        datasource.connection.use { connection ->
            val statement =
                connection.prepareStatement(
                    """
                    SELECT behandling_id
                    FROM beregning_oppdatering
                    WHERE hentet_status = ?
                    LIMIT ?
                    """.trimIndent(),
                )
            statement.setString(1, HentetStatus.IKKE_HENTET.name)
            statement.setInt(2, limit)

            val resultSet = statement.executeQuery()
            resultSet.toList {
                this.getObject("behandling_id", UUID::class.java)
            }
        }

    fun lagreBeregning(
        behandlingId: UUID,
        beregning: Beregning?,
    ): Boolean =
        datasource.connection.use { connection ->
            val statement =
                connection.prepareStatement(
                    """
                    UPDATE beregning_oppdatering SET beregning_behandling = ?, hentet_status = ? 
                    WHERE behandling_id = ?
                    """.trimIndent(),
                )

            statement.setJsonb(1, beregning)
            if (beregning == null) {
                statement.setString(2, HentetStatus.IKKE_FUNNET.name)
            } else {
                statement.setString(2, HentetStatus.HENTET.name)
            }
            statement.setObject(3, behandlingId)
            statement.executeUpdate() == 1
        }

    fun lagrePatchetStatus(
        behandlingId: UUID,
        antallStoenadRadPatchet: Int,
        antallSakRadPatchet: Int,
        withConnection: Connection? = null,
    ): Boolean {
        val oppdatering = { connection: Connection ->
            val statement =
                connection.prepareStatement(
                    """
                    UPDATE beregning_oppdatering SET patchet_status = ?, antall_sak_fix = ?, antall_stoenad_fix = ? 
                    WHERE behandling_id = ?
                    """.trimIndent(),
                )

            statement.setString(1, PatchetStatus.PATCHET.name)
            statement.setInt(2, antallSakRadPatchet)
            statement.setInt(3, antallStoenadRadPatchet)
            statement.setObject(4, behandlingId)

            statement.executeUpdate() == 1
        }
        logger.info(
            "Patchet $antallSakRadPatchet sak-rader og $antallStoenadRadPatchet stoenad-rader " +
                "for beregningen for behandling med id=$behandlingId",
        )

        if (withConnection == null) {
            return datasource.connection.use(oppdatering)
        }
        return withConnection.let(oppdatering)
    }

    fun hentBehandlingerSomIkkeErOppdatert(sakerAvGangen: Int): List<Pair<UUID, Beregning?>> =
        datasource.connection.use { connection ->
            val statement =
                connection.prepareStatement(
                    """
                    SELECT behandling_id, beregning_behandling from beregning_oppdatering 
                    WHERE hentet_status = ?
                    AND patchet_status = ?
                    LIMIT ?
                    """.trimIndent(),
                )
            statement.setString(1, HentetStatus.HENTET.name)
            statement.setString(2, PatchetStatus.IKKE_PATCHET.name)
            statement.setInt(3, sakerAvGangen)
            statement.executeQuery().toList {
                val behandlingId = UUID.fromString(this.getString("behandling_id"))
                val utlandstilknytning =
                    getString("beregning_behandling")?.let { objectMapper.readValue<Beregning>(it) }
                behandlingId to utlandstilknytning
            }
        }

    fun hentBehandlingerForOppdateringAnvendtSats(sakerAvGangen: Int): List<BehandlingMedStatistikkShitSomSkalFiksesI> =
        datasource.connection.use { connection ->
            val statement =
                connection.prepareStatement(
                    """
                    SELECT behandling_id, beregning_behandling from beregning_oppdatering
                    where sats_oppdatert is false
                    limit ?
                    """.trimIndent(),
                )
            statement.setInt(1, sakerAvGangen)
            val behandlinger =
                statement.executeQuery().toList {
                    val behandlingId = UUID.fromString(getString("behandling_id"))
                    val beregning = getString("beregning_behandling")?.let { objectMapper.readValue<Beregning>(it) }
                    behandlingId to beregning
                }
            behandlinger.map { (id, beregning) ->
                BehandlingMedStatistikkShitSomSkalFiksesI(
                    behandlingId = id,
                    statistikkMaaneder = hentAktuelleStatistikkmaanederOgAvdoedForBehandling(id, connection),
                    beregning = beregning!!,
                )
            }
        }

    private fun hentAktuelleStatistikkmaanederOgAvdoedForBehandling(
        behandlingId: UUID,
        connection: Connection,
    ): List<Pair<YearMonth, Int>> {
        val statement =
            connection.prepareStatement(
                """
                SELECT statistikkmaaned, fnrforeldre from maaned_stoenad where behandlingid = ? and statistikkmaaned >= '2024-01'
                """.trimIndent(),
            )
        statement.setObject(1, behandlingId)
        return statement.executeQuery().toList {
            val statistikkMaaned = YearMonth.parse(getString("statistikkmaaned"))
            val foreldre: List<String>? = getString("fnrforeldre")?.let { objectMapper.readValue(it) }
            statistikkMaaned to (foreldre?.size ?: 0)
        }
    }

    fun oppdaterAnvendtSats(
        behandlingId: UUID,
        statistikkMaanederOgSatser: List<Pair<YearMonth, String>>,
    ) {
        datasource.connection.use { connection ->
            statistikkMaanederOgSatser.forEach { (maaned, sats) ->
                val statement =
                    connection.prepareStatement(
                        """
                        update maaned_stoenad set anvendtsats = ? where behandlingid = ? and statistikkmaaned = ?
                        """.trimIndent(),
                    )
                statement.setString(1, sats)
                statement.setObject(2, behandlingId)
                statement.setString(3, maaned.toString())
                statement.executeUpdate()
            }
            // lagre at vi har ryddet opp
            val statement =
                connection.prepareStatement(
                    """
                    update beregning_oppdatering set sats_oppdatert = true where behandling_id = ?
                    """.trimIndent(),
                )
            statement.setObject(1, behandlingId)
            statement.executeUpdate()
        }
    }

    fun patchRaderForBehandling(
        behandlingId: UUID,
        beregning: Beregning,
    ) = datasource.connection.use { connection ->
        val sakerOppdatering =
            connection.prepareStatement(
                """
                UPDATE sak SET beregning = ? WHERE behandling_id = ?
                """.trimIndent(),
            )
        sakerOppdatering.setJsonb(1, beregning)
        sakerOppdatering.setObject(2, behandlingId)
        val antallSakerOppdatert = sakerOppdatering.executeUpdate()

        val stoenadOppdatering =
            connection.prepareStatement(
                """
                UPDATE stoenad SET beregning = ? WHERE behandlingid = ?
                """.trimIndent(),
            )
        stoenadOppdatering.setJsonb(1, beregning)
        stoenadOppdatering.setObject(2, behandlingId)
        val antallStoenadOppdatert = stoenadOppdatering.executeUpdate()

        lagrePatchetStatus(
            behandlingId,
            antallStoenadOppdatert,
            antallSakerOppdatert,
            connection,
        )
    }
}

data class BehandlingMedStatistikkShitSomSkalFiksesI(
    val behandlingId: UUID,
    val statistikkMaaneder: List<Pair<YearMonth, Int>>,
    val beregning: Beregning,
)

enum class HentetStatus {
    IKKE_HENTET,
    HENTET,
    IKKE_FUNNET,
}

enum class PatchetStatus {
    IKKE_PATCHET,
    PATCHET,
}
