package no.nav.etterlatte.statistikk.database

import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.database.toList
import no.nav.etterlatte.statistikk.domain.SakUtland
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.util.UUID
import javax.sql.DataSource

class RyddUtlandstilsnittDao(
    private val datasource: DataSource,
) {
    private val connection get() = datasource.connection
    private val logger = LoggerFactory.getLogger(this::class.java)

    companion object {
        fun using(datasource: DataSource): RyddUtlandstilsnittDao {
            return RyddUtlandstilsnittDao(datasource)
        }
    }

    fun hentBehandlingerMedManglendeUtlandstilsnitt(limit: Int): List<UUID> {
        return connection.use { connection ->
            val statement =
                connection.prepareStatement(
                    """
                    SELECT behandling_id
                    FROM utlandstilknytning_fiksing
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

    fun lagreUtlandstilknytning(
        behandlingId: UUID,
        utlandstilknytning: UtlandstilknytningType?,
    ): Boolean {
        return connection.use { connection ->
            val statement =
                connection.prepareStatement(
                    """
                    UPDATE utlandstilknytning_fiksing SET utlandstilknytning_behandling = ?, hentet_status = ? 
                    WHERE behandling_id = ?
                    """.trimIndent(),
                )

            if (utlandstilknytning == null) {
                statement.setString(1, null)
                statement.setString(2, HentetStatus.IKKE_FUNNET.name)
            } else {
                statement.setString(1, utlandstilknytning.name)
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
                    UPDATE utlandstilknytning_fiksing SET patch_status = ?, antall_sak_fix = ?, antall_stoenad_fix = ? 
                    WHERE behandling_id = ?
                    """.trimIndent(),
                )

            statement.setString(1, PatchStatus.PATCHET.name)
            statement.setInt(2, antallSakRadPatchet)
            statement.setInt(3, antallStoenadRadPatchet)
            statement.setObject(4, behandlingId)

            statement.executeUpdate() == 1
        }

        logger.info(
            "Patchet $antallSakRadPatchet sak-rader og $antallStoenadRadPatchet stoenad-rader " +
                "for behandling med id=$behandlingId",
        )

        if (withConnection == null) {
            return connection.use(oppdatering)
        }
        return withConnection.let(oppdatering)
    }

    fun hentBehandlingerSomIkkeErPatchet(sakerAvGangen: Int): List<Pair<UUID, UtlandstilknytningType>> {
        return connection.use { connection ->
            val statement =
                connection.prepareStatement(
                    """
                    SELECT behandling_id, utlandstilknytning_behandling from utlandstilknytning_fiksing 
                    WHERE hentet_status = ?
                    AND patchet_status = ?
                    LIMIT ?
                    """.trimIndent(),
                )
            statement.setString(1, HentetStatus.HENTET.name)
            statement.setString(2, PatchStatus.IKKE_PATCHET.name)
            statement.setInt(3, sakerAvGangen)
            statement.executeQuery().toList {
                val behandlingId = UUID.fromString(this.getString("behandling_id"))
                val utlandstilknytning =
                    enumValueOf<UtlandstilknytningType>(
                        this.getString("utlandstilknytning_behandling"),
                    )
                behandlingId to utlandstilknytning
            }
        }
    }

    fun patchRaderForBehandling(
        behandlingId: UUID,
        statistikkUtlandstilknytning: SakUtland,
    ) {
        return connection.use { connection ->
            val sakerOppdatering =
                connection.prepareStatement(
                    """
                    UPDATE sak SET sak_utland = ? WHERE behandling_id = ?
                    """.trimIndent(),
                )
            sakerOppdatering.setString(1, statistikkUtlandstilknytning.name)
            sakerOppdatering.setObject(2, behandlingId)
            val antallSakerOppdatert = sakerOppdatering.executeUpdate()

            val stoenadOppdatering =
                connection.prepareStatement(
                    """
                    UPDATE stoenad SET sak_utland = ? WHERE behandlingid = ?
                    """.trimIndent(),
                )
            stoenadOppdatering.setString(1, statistikkUtlandstilknytning.name)
            stoenadOppdatering.setObject(2, behandlingId)
            val antallStoenadOppdatert = stoenadOppdatering.executeUpdate()

            val maanedStoenadOppdatering =
                connection.prepareStatement(
                    """
                    UPDATE maaned_stoenad SET sak_utland = ? WHERE behandlingid = ?
                    """.trimIndent(),
                )
            maanedStoenadOppdatering.setString(1, statistikkUtlandstilknytning.name)
            maanedStoenadOppdatering.setObject(2, behandlingId)
            val antallMaanedStoenadOppdatert = maanedStoenadOppdatering.executeUpdate()

            lagrePatchetStatus(
                behandlingId,
                antallStoenadOppdatert + antallMaanedStoenadOppdatert,
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

enum class PatchStatus {
    IKKE_PATCHET,
    PATCHET,
}
