package no.nav.etterlatte.behandling.behandlinginfo

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.behandling.utland.SluttbehandlingUtlandBehandlinginfo
import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.libs.common.behandling.Brevutfall
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.checkInternFeil
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.database.setJsonb
import no.nav.etterlatte.libs.database.singleOrNull
import java.util.UUID

class BehandlingInfoDao(
    private val connectionAutoclosing: ConnectionAutoclosing,
) {
    fun lagreErOmgjoeringSluttbehandlingUtland(id: UUID) {
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
                    setBoolean(2, true)
                }.run { executeUpdate() }
            }
        }
    }

    fun lagreSluttbehandling(
        behandlingId: UUID,
        sluttbehandling: SluttbehandlingUtlandBehandlinginfo,
    ) {
        connectionAutoclosing.hentConnection { connection ->
            with(connection) {
                prepareStatement(
                    """
                    INSERT INTO behandling_info(behandling_id, sluttbehandling)
                    VALUES (?, ?)
                    ON CONFLICT (behandling_id) DO 
                    UPDATE SET sluttbehandling = excluded.sluttbehandling
                    """.trimIndent(),
                ).let { statement ->
                    statement.setObject(1, behandlingId)
                    statement.setJsonb(2, sluttbehandling)
                    statement.executeUpdate()
                }
            }
        }
    }

    fun hentSluttbehandling(behandlingId: UUID): SluttbehandlingUtlandBehandlinginfo? =
        connectionAutoclosing.hentConnection { connection ->
            with(connection) {
                prepareStatement(
                    """
                    SELECT sluttbehandling 
                    FROM behandling_info 
                    WHERE behandling_id = ?::UUID AND sluttbehandling IS NOT NULL
                    """.trimIndent(),
                ).let { statement ->
                    statement.setObject(1, behandlingId)
                    statement.executeQuery().singleOrNull {
                        getString("sluttbehandling").let { objectMapper.readValue(it) }
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
                    .also {
                        checkInternFeil(it == 1) {
                            "Kunne ikke lagreBrevutfall behandling for ${brevutfall.behandlingId}"
                        }
                    }.let {
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
                    SELECT brevutfall 
                    FROM behandling_info 
                    WHERE behandling_id = ?::UUID AND brevutfall IS NOT NULL
                    """,
                ).apply { setObject(1, behandlingId) }
                    .run {
                        executeQuery().singleOrNull {
                            getString("brevutfall").let { objectMapper.readValue(it) }
                        }
                    }
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
                    .also {
                        checkInternFeil(it == 1) {
                            "Kunne ikke lagreBrevutfall behandling for ${etterbetaling.behandlingId}"
                        }
                    }.let {
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
                    WHERE behandling_id = ?::UUID
                    """.trimIndent(),
                ).apply {
                    setJsonb(1, null)
                    setObject(2, behandlingId)
                }.run { executeUpdate() }
                    .also {
                        checkInternFeil(it == 1) {
                            "Kunne ikke slettEtterbetaling behandling for $behandlingId"
                        }
                    }
            }
        }

    fun hentEtterbetaling(behandlingId: UUID): Etterbetaling? =
        connectionAutoclosing.hentConnection {
            with(it) {
                prepareStatement(
                    """
                    SELECT etterbetaling 
                    FROM behandling_info 
                    WHERE behandling_id = ?::UUID AND etterbetaling IS NOT NULL
                    """,
                ).apply { setObject(1, behandlingId) }
                    .run { executeQuery().singleOrNull { getString("etterbetaling").let { objectMapper.readValue(it) } } }
            }
        }
}
