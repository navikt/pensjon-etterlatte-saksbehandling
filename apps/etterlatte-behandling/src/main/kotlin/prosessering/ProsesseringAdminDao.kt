package no.nav.etterlatte.prosessering

import efterlatte.prosessering.Status
import efterlatte.prosessering.Stoppaarsak
import efterlatte.prosessering.Task
import java.sql.ResultSet
import javax.sql.DataSource

/** Read-only DAO over prosessering_task, brukt av efterlatte-verktoy. */
class ProsesseringAdminDao(
    private val dataSource: DataSource,
) {
    private val tabell = "public.prosessering_task"

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
}
