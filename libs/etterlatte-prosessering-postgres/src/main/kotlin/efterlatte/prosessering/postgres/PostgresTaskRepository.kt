package efterlatte.prosessering.postgres

import efterlatte.prosessering.Status
import efterlatte.prosessering.Stoppaarsak
import efterlatte.prosessering.Task
import efterlatte.prosessering.TaskRepository
import efterlatte.prosessering.Transaksjon
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import javax.sql.DataSource

/**
 * Postgres-implementasjon av [TaskRepository]. Ren JDBC (ingen Spring Data),
 * slik at samme SQL kan tjene alle adaptere.
 *
 * Tabellene bor i et eget [skjema] (default `prosessering`) i vertens database.
 * Eget skjema holder bibliotekets tabeller logisk adskilt fra vertens domene-
 * skjema — outbox-garantien er upåvirket, siden `opprett` fortsatt skriver på
 * samme connection/transaksjon som forretnings-skrivet (samme database).
 *
 * Plukk-strategien låner db-schedulers `SELECT … FOR UPDATE SKIP LOCKED`-teknikk
 * uten avhengigheten: én atomisk CTE-update flipper klare rader til KJØRER og
 * committer, som er det som gjør at andre pods hopper over dem.
 */
class PostgresTaskRepository(
    private val dataSource: DataSource,
    skjema: String = "prosessering",
) : TaskRepository {
    private val tabell = "$skjema.task"

    override fun claimBatch(limit: Int): List<Task> =
        dataSource.connection.use { connection ->
            connection.prepareStatement(claimBatchSql).use { statement ->
                statement.setInt(1, limit)
                statement.executeQuery().use { resultSet ->
                    buildList {
                        while (resultSet.next()) add(resultSet.tilTask())
                    }
                }
            }
        }

    override fun <T> iEgenTransaksjon(block: (Transaksjon) -> T): T =
        dataSource.connection.use { connection ->
            val forrigeAutoCommit = connection.autoCommit
            connection.autoCommit = false
            try {
                val resultat = block(JdbcTransaksjon(connection))
                connection.commit()
                resultat
            } catch (e: Throwable) {
                connection.rollback()
                throw e
            } finally {
                connection.autoCommit = forrigeAutoCommit
            }
        }

    override fun markerFullført(
        transaksjon: Transaksjon,
        id: Long,
    ) {
        transaksjon.connection().prepareStatement(markFullførtSql).use { statement ->
            statement.setLong(1, id)
            statement.executeUpdate()
        }
    }

    override fun markFeilet(
        id: Long,
        nyStatus: Status,
        stoppaarsak: Stoppaarsak?,
        nesteTriggerTid: Instant,
    ) = dataSource.connection.use { connection ->
        connection.prepareStatement(markFeiletSql).use { statement ->
            statement.setString(1, nyStatus.name)
            statement.setString(2, stoppaarsak?.name)
            statement.setTimestamp(3, Timestamp.from(nesteTriggerTid))
            statement.setLong(4, id)
            statement.executeUpdate()
            Unit
        }
    }

    override fun gjenopprettHengende(plukketFoer: Instant): Int =
        dataSource.connection.use { connection ->
            connection.prepareStatement(gjenopprettHengendeSql).use { statement ->
                statement.setTimestamp(1, Timestamp.from(plukketFoer))
                statement.executeUpdate()
            }
        }

    override fun finn(id: Long): Task? =
        dataSource.connection.use { connection ->
            connection.prepareStatement("SELECT * FROM $tabell WHERE id = ?").use { statement ->
                statement.setLong(1, id)
                statement.executeQuery().use { resultSet ->
                    if (resultSet.next()) resultSet.tilTask() else null
                }
            }
        }

    override fun antallMedStatus(status: Status): Int =
        dataSource.connection.use { connection ->
            connection.prepareStatement("SELECT count(*) FROM $tabell WHERE status = ?").use { statement ->
                statement.setString(1, status.name)
                statement.executeQuery().use { resultSet ->
                    resultSet.next()
                    resultSet.getInt(1)
                }
            }
        }

    override fun insert(
        transaksjon: Transaksjon,
        type: String,
        payload: String?,
        triggerTid: Instant,
    ): Long = insertMed(transaksjon.connection(), type, payload, triggerTid)

    override fun insertFrittstaaende(
        type: String,
        payload: String?,
        triggerTid: Instant,
    ): Long = iEgenTransaksjon { transaksjon -> insert(transaksjon, type, payload, triggerTid) }

    private fun insertMed(
        connection: Connection,
        type: String,
        payload: String?,
        triggerTid: Instant,
    ): Long =
        connection.prepareStatement(insertSql).use { statement ->
            statement.setString(1, type)
            statement.setString(2, Status.KLAR.name)
            statement.setString(3, payload)
            statement.setTimestamp(4, Timestamp.from(triggerTid))
            statement.executeQuery().use { resultSet ->
                resultSet.next()
                resultSet.getLong("id")
            }
        }

    private fun Transaksjon.connection(): Connection =
        (this as? JdbcTransaksjon)?.connection
            ?: error("Forventet JdbcTransaksjon, fikk ${this::class.simpleName}")

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

    private val claimBatchSql =
        """
        WITH klar AS (
            SELECT id FROM $tabell
             WHERE status = 'KLAR' AND trigger_tid <= now()
             ORDER BY trigger_tid LIMIT ?
             FOR UPDATE SKIP LOCKED
        )
        UPDATE $tabell SET status = 'KJØRER', plukket_tid = now(), versjon = versjon + 1
         WHERE id IN (SELECT id FROM klar) RETURNING *;
        """.trimIndent()

    private val markFullførtSql =
        "UPDATE $tabell SET status = 'FULLFØRT', versjon = versjon + 1 WHERE id = ?"

    private val gjenopprettHengendeSql =
        """
        UPDATE $tabell
           SET status = 'KLAR', plukket_tid = NULL, versjon = versjon + 1
         WHERE status = 'KJØRER' AND plukket_tid < ?
        """.trimIndent()

    private val markFeiletSql =
        """
        UPDATE $tabell
           SET status = ?, stoppaarsak = ?, antall_feil = antall_feil + 1,
               trigger_tid = ?, versjon = versjon + 1
         WHERE id = ?
        """.trimIndent()

    private val insertSql =
        """
        INSERT INTO $tabell (type, status, payload, trigger_tid)
        VALUES (?, ?, ?, ?) RETURNING id
        """.trimIndent()
}
