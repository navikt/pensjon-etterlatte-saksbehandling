package no.nav.etterlatte.statistikk.database

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.database.toList
import no.nav.etterlatte.statistikk.domain.Beregning
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.util.UUID
import javax.sql.DataSource

class RefreshBeregningDao(
    private val datasource: DataSource,
) {
    private val connection get() = datasource.connection
    private val logger = LoggerFactory.getLogger(this::class.java)

    companion object {
        fun using(datasource: DataSource): RefreshBeregningDao {
            return RefreshBeregningDao(datasource)
        }
    }

    fun hentBehandlingerUtenOppdatertBeregning(limit: Int): List<UUID> {
        return connection.use { connection ->
            val statement =
                connection.prepareStatement(
                    """
                    SELECT behandling_id
                    FROM beregning_refresh
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
    }

    fun lagreBeregning(
        behandlingId: UUID,
        beregning: Beregning?,
    ): Boolean {
        return connection.use { connection ->
            val statement =
                connection.prepareStatement(
                    """
                    UPDATE beregning_refresh SET beregning = ?, hentet_status = ? 
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
                    UPDATE beregning_refresh SET patchet_status = ?, antall_sak_fix = ?, antall_stoenad_fix = ? 
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
            return connection.use(oppdatering)
        }
        return withConnection.let(oppdatering)
    }

    fun hentBehandlingerSomIkkeErRefreshet(sakerAvGangen: Int): List<Pair<UUID, Beregning?>> {
        return connection.use { connection ->
            val statement =
                connection.prepareStatement(
                    """
                    SELECT behandling_id, beregning_behandling from beregning_refresh 
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
                val utlandstilknytning = getString("beregning_behandling")?.let { objectMapper.readValue<Beregning>(it) }
                behandlingId to utlandstilknytning
            }
        }
    }

    fun patchRaderForBehandling(
        behandlingId: UUID,
        beregning: Beregning,
    ) {
        return connection.use { connection ->
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
}

enum class HentetStatus {
    IKKE_HENTET,
    HENTET,
    IKKE_FUNNET,
}

enum class PatchetStatus {
    IKKE_PATCHET,
    PATCHET,
}
