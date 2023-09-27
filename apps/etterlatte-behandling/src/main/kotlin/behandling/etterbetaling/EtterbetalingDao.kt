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
                    INSERT INTO etterbetaling(behandlingId, fraDato, tilDato)
                    VALUES (?, ?, ?)
                    ON CONFLICT (behandlingId) DO UPDATE SET fraDato = excluded.fraDato, tilDato = excluded.tilDato
                    """.trimIndent(),
                )
            statement.setObject(1, etterbetaling.behandlingsId)
            statement.setDate(2, etterbetaling.fraDato.let { Date.valueOf(it) })
            statement.setDate(3, etterbetaling.tilDato.let { Date.valueOf(it) })
            statement.executeUpdate()
        }
    }

    fun hentEtterbetaling(behandlingsId: UUID): Etterbetalingmodell? =
        with(connection()) {
            val statement =
                connection().prepareStatement(
                    "SELECT fra_dato, til_dato FROM etterbetaling WHERE behandlings_id = ?::UUID",
                )
            statement.setObject(1, behandlingsId)

            return statement.executeQuery().singleOrNull {
                Etterbetalingmodell(
                    behandlingsId = behandlingsId,
                    fraDato = getDate("fra_dato").toLocalDate(),
                    tilDato = getDate("til_dato").toLocalDate(),
                )
            }
        }
}
