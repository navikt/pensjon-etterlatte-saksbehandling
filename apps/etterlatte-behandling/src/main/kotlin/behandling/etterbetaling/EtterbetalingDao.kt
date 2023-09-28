package no.nav.etterlatte.behandling.etterbetaling

import no.nav.etterlatte.libs.common.behandling.Etterbetalingmodell
import no.nav.etterlatte.libs.database.singleOrNull
import java.sql.Connection
import java.sql.Date
import java.util.UUID

class EtterbetalingDao(private val connection: () -> Connection) {
    fun lagreEtterbetaling(etterbetaling: Etterbetalingmodell) {
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
            statement.setDate(2, etterbetaling.fraDato.let { Date.valueOf(it) })
            statement.setDate(3, etterbetaling.tilDato.let { Date.valueOf(it) })
            statement.executeUpdate()
        }
    }

    fun hentEtterbetaling(behandlingId: UUID): Etterbetalingmodell? =
        with(connection()) {
            val statement =
                connection().prepareStatement(
                    "SELECT fra_dato, til_dato FROM etterbetaling WHERE behandling_id = ?::UUID",
                )
            statement.setObject(1, behandlingId)

            return statement.executeQuery().singleOrNull {
                Etterbetalingmodell(
                    behandlingId = behandlingId,
                    fraDato = getDate("fra_dato").toLocalDate(),
                    tilDato = getDate("til_dato").toLocalDate(),
                )
            }
        }
}
