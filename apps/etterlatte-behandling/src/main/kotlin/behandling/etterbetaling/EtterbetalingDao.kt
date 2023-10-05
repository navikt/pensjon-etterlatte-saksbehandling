package no.nav.etterlatte.behandling.etterbetaling

import no.nav.etterlatte.libs.common.behandling.Etterbetaling
import no.nav.etterlatte.libs.database.singleOrNull
import java.sql.Connection
import java.sql.Date
import java.time.YearMonth
import java.util.UUID

class EtterbetalingDao(private val connection: () -> Connection) {
    fun lagreEtterbetaling(etterbetaling: Etterbetaling) {
        with(connection()) {
            val statement =
                prepareStatement(
                    """
                    INSERT INTO etterbetaling(behandling_id, fra_dato, til_dato)
                    VALUES (?, ?, ?)
                    ON CONFLICT (behandling_id) DO UPDATE SET fra_dato = excluded.fra_dato, til_dato = excluded.til_dato
                    """.trimIndent(),
                )
            statement.setObject(1, etterbetaling.behandlingId)
            statement.setDate(2, etterbetaling.fra.atDay(1).let { Date.valueOf(it) })
            statement.setDate(3, etterbetaling.til.atEndOfMonth().let { Date.valueOf(it) })
            statement.executeUpdate()
        }
    }

    fun hentEtterbetaling(behandlingId: UUID): Etterbetaling? =
        with(connection()) {
            val statement =
                prepareStatement(
                    "SELECT fra_dato, til_dato FROM etterbetaling WHERE behandling_id = ?::UUID",
                )
            statement.setObject(1, behandlingId)

            return statement.executeQuery().singleOrNull {
                Etterbetaling(
                    behandlingId = behandlingId,
                    fra = getDate("fra_dato").toLocalDate().let { YearMonth.from(it) },
                    til = getDate("til_dato").toLocalDate().let { YearMonth.from(it) },
                )
            }
        }

    fun slettEtterbetaling(behandlingsId: UUID) {
        with(connection()) {
            val statement =
                prepareStatement(
                    "DELETE FROM etterbetaling WHERE behandling_id = ?::UUID".trimIndent(),
                )
            statement.setObject(1, behandlingsId)
            statement.executeUpdate()
        }
    }
}
