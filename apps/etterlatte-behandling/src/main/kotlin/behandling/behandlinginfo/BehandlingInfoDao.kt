package no.nav.etterlatte.behandling.behandlinginfo

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.setTidspunkt
import no.nav.etterlatte.libs.database.setJsonb
import no.nav.etterlatte.libs.database.singleOrNull
import java.sql.Connection
import java.sql.ResultSet
import java.util.UUID

class BehandlingInfoDao(private val connection: () -> Connection) {
    fun lagreBrevutfall(brevutfall: Brevutfall): Brevutfall {
        return connection().prepareStatement(
            """
            INSERT INTO behandling_info(behandling_id, oppdatert, brevutfall)
            VALUES (?, ?, ?)
            ON CONFLICT (behandling_id) DO 
            UPDATE SET oppdatert = excluded.oppdatert, brevutfall = excluded.brevutfall
            """.trimIndent(),
        )
            .apply {
                setObject(1, brevutfall.behandlingId)
                setTidspunkt(2, Tidspunkt.now())
                setJsonb(3, brevutfall)
            }
            .run { executeUpdate() }
            .also { require(it == 1) }
            .let { hentBrevutfall(brevutfall.behandlingId) ?: throw InternfeilException("Feilet under lagring av brevutfall") }
    }

    fun hentBrevutfall(behandlingId: UUID): Brevutfall? {
        return connection()
            .prepareStatement(
                """
                    SELECT behandling_id, brevutfall 
                    FROM behandling_info 
                    WHERE behandling_id = ?::UUID
                    """,
            )
            .apply { setObject(1, behandlingId) }
            .run { executeQuery().singleOrNull { toBrevutfall() } }
    }

    fun lagreEtterbetaling(etterbetaling: EtterbetalingNy): EtterbetalingNy {
        return connection().prepareStatement(
            """
            INSERT INTO behandling_info(behandling_id, oppdatert, etterbetaling)
            VALUES (?, ?, ?)
            ON CONFLICT (behandling_id) DO 
            UPDATE SET oppdatert = excluded.oppdatert, etterbetaling = excluded.etterbetaling
            """.trimIndent(),
        )
            .apply {
                setObject(1, etterbetaling.behandlingId)
                setTidspunkt(2, Tidspunkt.now())
                setJsonb(3, etterbetaling)
            }
            .run { executeUpdate() }
            .also { require(it == 1) }
            .let {
                hentEtterbetaling(etterbetaling.behandlingId)
                    ?: throw InternfeilException("Feilet under lagring av etterbetaling")
            }
    }

    fun slettEtterbetaling(behandlingId: UUID): Int {
        return connection().prepareStatement(
            """
            UPDATE behandling_info SET oppdatert = ?, etterbetaling = ?
            WHERE behandling_id = ?
            """.trimIndent(),
        )
            .apply {
                setTidspunkt(1, Tidspunkt.now())
                setJsonb(2, null)
                setObject(3, behandlingId)
            }
            .run { executeUpdate() }
            .also { require(it == 1) }
    }

    fun hentEtterbetaling(behandlingId: UUID): EtterbetalingNy? {
        return connection()
            .prepareStatement(
                """
                    SELECT behandling_id, etterbetaling 
                    FROM behandling_info 
                    WHERE behandling_id = ?::UUID AND etterbetaling IS NOT NULL
                    """,
            )
            .apply { setObject(1, behandlingId) }
            .run { executeQuery().singleOrNull { toEtterbetaling() } }
    }

    private fun ResultSet.toBrevutfall(): Brevutfall = this.getString("brevutfall").let { objectMapper.readValue(it) }

    private fun ResultSet.toEtterbetaling(): EtterbetalingNy = this.getString("etterbetaling").let { objectMapper.readValue(it) }
}
