package no.nav.etterlatte.behandling.behandlinginfo

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.libs.common.behandling.Brevutfall
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.database.setJsonb
import no.nav.etterlatte.libs.database.singleOrNull
import java.sql.ResultSet
import java.util.UUID

class BehandlingInfoDao(
    private val connectionAutoclosing: ConnectionAutoclosing,
) {
    fun lagreOmgjoeringSluttbehandlingUtland(
        id: UUID,
        sluttbehandlingUtland: Boolean,
    ) {
        connectionAutoclosing.hentConnection { connection ->
            with(connection) {
                prepareStatement(
                    """
                    INSERT INTO behandling_info(behandling_id, omgjoering_sluttbehandling_utland)
                    VALUES (?, ?)
                    ON CONFLICT (behandling_id) DO 
                    UPDATE SET omgjoering_sluttbehandling_utland = excluded.omgjoering_sluttbehandling_utland
                    """.trimIndent(),
                ).apply {
                    setObject(1, id)
                    setJsonb(2, sluttbehandlingUtland)
                }.run { executeUpdate() }
            }
        }
    }

    fun hentErOmgjoeringSluttbehandlingUtland(behandlingId: UUID): Boolean =
        connectionAutoclosing.hentConnection {
            with(it) {
                prepareStatement(
                    """
                SELECT omgjoering_sluttbehandling_utland 
                FROM behandling_info 
                WHERE behandling_id = ?
                """,
                ).apply { setObject(1, behandlingId) }
                    .executeQuery()
                    .run {
                        if (next()) {
                            getBoolean("omgjoering_sluttbehandling_utland")
                        } else {
                            throw InternfeilException("Fant ingen data for OmgjoeringSluttbehandlingUtland for behandlingId: $behandlingId")
                        }
                    }
            }
        }

    fun lagreBrevutfall(brevutfall: Brevutfall): Brevutfall =
        connectionAutoclosing.hentConnection { connection ->
            with(connection) {
                prepareStatement(
                    """
                    INSERT INTO behandling_info(behandling_id, brevutfall)
                    VALUES (?, ?)
                    ON CONFLICT (behandling_id) DO 
                    UPDATE SET brevutfall = excluded.brevutfall
                    """.trimIndent(),
                ).apply {
                    setObject(1, brevutfall.behandlingId)
                    setJsonb(2, brevutfall)
                }.run { executeUpdate() }
                    .also { require(it == 1) }
                    .let {
                        hentBrevutfall(brevutfall.behandlingId)
                            ?: throw InternfeilException("Feilet under lagring av brevutfall")
                    }
            }
        }

    fun hentBrevutfall(behandlingId: UUID): Brevutfall? =
        connectionAutoclosing.hentConnection {
            with(it) {
                prepareStatement(
                    """
                    SELECT behandling_id, brevutfall 
                    FROM behandling_info 
                    WHERE behandling_id = ?::UUID
                    """,
                ).apply { setObject(1, behandlingId) }
                    .run { executeQuery().singleOrNull { toBrevutfall() } }
            }
        }

    fun lagreEtterbetaling(etterbetaling: Etterbetaling): Etterbetaling =
        connectionAutoclosing.hentConnection { connection ->
            with(connection) {
                prepareStatement(
                    """
                    INSERT INTO behandling_info(behandling_id, etterbetaling)
                    VALUES (?, ?)
                    ON CONFLICT (behandling_id) DO 
                    UPDATE SET etterbetaling = excluded.etterbetaling
                    """.trimIndent(),
                ).apply {
                    setObject(1, etterbetaling.behandlingId)
                    setJsonb(2, etterbetaling)
                }.run { executeUpdate() }
                    .also { require(it == 1) }
                    .let {
                        hentEtterbetaling(etterbetaling.behandlingId)
                            ?: throw InternfeilException("Feilet under lagring av etterbetaling")
                    }
            }
        }

    fun slettEtterbetaling(behandlingId: UUID): Int =
        connectionAutoclosing.hentConnection { connection ->
            with(connection) {
                prepareStatement(
                    """
                    UPDATE behandling_info SET etterbetaling = ?
                    WHERE behandling_id = ?
                    """.trimIndent(),
                ).apply {
                    setJsonb(1, null)
                    setObject(2, behandlingId)
                }.run { executeUpdate() }
                    .also { require(it == 1) }
            }
        }

    fun hentEtterbetaling(behandlingId: UUID): Etterbetaling? =
        connectionAutoclosing.hentConnection {
            with(it) {
                prepareStatement(
                    """
                    SELECT behandling_id, etterbetaling 
                    FROM behandling_info 
                    WHERE behandling_id = ?::UUID AND etterbetaling IS NOT NULL
                    """,
                ).apply { setObject(1, behandlingId) }
                    .run { executeQuery().singleOrNull { toEtterbetaling() } }
            }
        }

    private fun ResultSet.toBrevutfall(): Brevutfall = this.getString("brevutfall").let { objectMapper.readValue(it) }

    private fun ResultSet.toEtterbetaling(): Etterbetaling = this.getString("etterbetaling").let { objectMapper.readValue(it) }
}
