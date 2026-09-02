package no.nav.etterlatte.prosessering

import efterlatte.prosessering.Status
import efterlatte.prosessering.Stoppaarsak
import efterlatte.prosessering.Task
import efterlatte.prosessering.TaskStateMachine
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeFunnetException
import java.sql.Connection
import java.sql.ResultSet
import javax.sql.DataSource

enum class OperatorHandling(
    val nyStatus: Status,
    val verb: String,
) {
    REKJOER(nyStatus = Status.KLAR, verb = "rekjøres"),
    AVBRYT(nyStatus = Status.AVBRUTT, verb = "avbrytes"),
}

class TaskIkkeFunnet(
    id: Long,
) : IkkeFunnetException(
        code = "PROSESSERING_TASK_IKKE_FUNNET",
        detail = "Fant ingen task med id $id",
    )

class TaskEndretAvAndre(
    id: Long,
    forventetVersjon: Long,
    faktiskVersjon: Long,
) : ForespoerselException(
        status = 409,
        code = "PROSESSERING_TASK_ENDRET",
        detail =
            "Task $id er endret av noen andre. Du så versjon $forventetVersjon, " +
                "men den står nå på versjon $faktiskVersjon. Hent tasken på nytt og prøv igjen.",
    )

class UlovligTaskOvergang(
    id: Long,
    status: Status,
    handling: OperatorHandling,
) : ForespoerselException(
        status = 409,
        code = "PROSESSERING_ULOVLIG_OVERGANG",
        detail = "Task $id har status $status og kan ikke ${handling.verb}",
    )

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

    fun rekjoer(
        id: Long,
        forventetVersjon: Long,
    ): Task = utfoer(id = id, forventetVersjon = forventetVersjon, handling = OperatorHandling.REKJOER)

    fun avbryt(
        id: Long,
        forventetVersjon: Long,
    ): Task = utfoer(id = id, forventetVersjon = forventetVersjon, handling = OperatorHandling.AVBRYT)

    private fun utfoer(
        id: Long,
        forventetVersjon: Long,
        handling: OperatorHandling,
    ): Task =
        dataSource.connection.use { connection ->
            connection.autoCommit = false
            try {
                val gjeldende = finnForOppdatering(connection = connection, id = id) ?: throw TaskIkkeFunnet(id)

                // Versjonen sjekkes før statusen. Er versjonen utdatert, er statusen operatøren
                // så det også, og da er «noen andre rakk å endre den» det ærlige svaret — ikke
                // en påstand om en status vedkommende aldri har sett.
                if (gjeldende.versjon != forventetVersjon) {
                    throw TaskEndretAvAndre(
                        id = id,
                        forventetVersjon = forventetVersjon,
                        faktiskVersjon = gjeldende.versjon,
                    )
                }
                if (!TaskStateMachine.erLovlig(fra = gjeldende.status, til = handling.nyStatus)) {
                    throw UlovligTaskOvergang(id = id, status = gjeldende.status, handling = handling)
                }

                val oppdatert =
                    skriv(
                        connection = connection,
                        id = id,
                        forventetVersjon = forventetVersjon,
                        handling = handling,
                    )
                connection.commit()
                oppdatert
            } catch (feil: Throwable) {
                connection.rollback()
                throw feil
            }
        }

    private fun finnForOppdatering(
        connection: Connection,
        id: Long,
    ): Task? =
        connection.prepareStatement("SELECT * FROM $tabell WHERE id = ? FOR UPDATE").use { statement ->
            statement.setLong(1, id)
            statement.executeQuery().use { resultSet ->
                if (resultSet.next()) resultSet.tilTask() else null
            }
        }

    private fun skriv(
        connection: Connection,
        id: Long,
        forventetVersjon: Long,
        handling: OperatorHandling,
    ): Task {
        val felter =
            when (handling) {
                OperatorHandling.REKJOER -> {
                    "status = ?, trigger_tid = now(), plukket_tid = NULL, stoppaarsak = NULL"
                }

                OperatorHandling.AVBRYT -> {
                    "status = ?"
                }
            }
        val sql =
            "UPDATE $tabell SET $felter, versjon = versjon + 1 " +
                "WHERE id = ? AND versjon = ? RETURNING *"

        return connection.prepareStatement(sql).use { statement ->
            statement.setString(1, handling.nyStatus.name)
            statement.setLong(2, id)
            statement.setLong(3, forventetVersjon)
            statement.executeQuery().use { resultSet ->
                // Raden er låst av SELECT ... FOR UPDATE over, så at ingen rad treffes her ville
                // betydd at låsen ikke holdt. Da er rollback riktigere enn å svare noe som helst.
                check(resultSet.next()) { "Oppdatering av task $id traff ingen rad" }
                resultSet.tilTask()
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
