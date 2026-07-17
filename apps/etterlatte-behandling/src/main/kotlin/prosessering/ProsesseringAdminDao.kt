package no.nav.etterlatte.prosessering

import efterlatte.prosessering.Status
import efterlatte.prosessering.Stoppaarsak
import efterlatte.prosessering.Task
import java.sql.ResultSet
import javax.sql.DataSource

/**
 * Enkel lese-/admin-DAO for operatør-innsyn i prosessering-tasker (PoC Fase 4c).
 *
 * Lever i `etterlatte-behandling` (host-en), ikke i biblioteket, fordi dette er en
 * operatør-funksjon over vertens DB — ikke gjenbrukbar motor-infra. Motoren og
 * produsenten går fortsatt via [efterlatte.prosessering.TaskRepository]; denne DAO-en
 * gjør bare lesing og en manuell «rekjør» som setter en [Status.STOPPET]/[Status.AVBRUTT]
 * task tilbake til [Status.KLAR].
 */
class ProsesseringAdminDao(
    private val dataSource: DataSource,
    skjema: String = "prosessering",
) {
    private val tabell = "$skjema.task"

    fun list(
        status: Status?,
        limit: Int,
    ): List<Task> =
        dataSource.connection.use { connection ->
            val sql =
                buildString {
                    append("SELECT * FROM $tabell")
                    if (status != null) append(" WHERE status = ?")
                    append(" ORDER BY opprettet_tid DESC LIMIT ?")
                }
            connection.prepareStatement(sql).use { statement ->
                var indeks = 1
                if (status != null) statement.setString(indeks++, status.name)
                statement.setInt(indeks, limit)
                statement.executeQuery().use { resultSet ->
                    buildList {
                        while (resultSet.next()) add(resultSet.tilTask())
                    }
                }
            }
        }

    fun finn(id: Long): Task? =
        dataSource.connection.use { connection ->
            connection.prepareStatement("SELECT * FROM $tabell WHERE id = ?").use { statement ->
                statement.setLong(1, id)
                statement.executeQuery().use { resultSet ->
                    if (resultSet.next()) resultSet.tilTask() else null
                }
            }
        }

    /**
     * Manuell rekjøring: setter en stoppet/avbrutt task tilbake til [Status.KLAR] og
     * nullstiller `antall_feil` og `stoppaarsak` slik at motoren får fulle retries igjen.
     * Returnerer `true` hvis en rad faktisk ble endret (dvs. tasken sto i STOPPET/AVBRUTT).
     */
    fun rekjor(id: Long): Boolean =
        dataSource.connection.use { connection ->
            connection.prepareStatement(rekjorSql).use { statement ->
                statement.setLong(1, id)
                statement.executeUpdate() > 0
            }
        }

    private fun ResultSet.tilTask(): Task =
        Task(
            id = getLong("id"),
            type = getString("type"),
            status = Status.valueOf(getString("status")),
            payload = getString("payload"),
            triggerTid = getTimestamp("trigger_tid").toInstant(),
            opprettetTid = getTimestamp("opprettet_tid").toInstant(),
            plukketTid = getTimestamp("plukket_tid")?.toInstant(),
            antallFeil = getInt("antall_feil"),
            stoppaarsak = getString("stoppaarsak")?.let { Stoppaarsak.valueOf(it) },
            versjon = getLong("versjon"),
        )

    private val rekjorSql =
        """
        UPDATE $tabell
           SET status = 'KLAR', stoppaarsak = NULL, antall_feil = 0, plukket_tid = NULL,
               trigger_tid = now(), versjon = versjon + 1
         WHERE id = ? AND status IN ('STOPPET', 'AVBRUTT')
        """.trimIndent()
}
