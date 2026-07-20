package no.nav.etterlatte.prosessering

import efterlatte.prosessering.StandardTaskProdusent
import efterlatte.prosessering.Status
import efterlatte.prosessering.postgres.PostgresTaskRepository
import efterlatte.prosessering.strengType
import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.etterlatte.libs.database.transaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import javax.sql.DataSource

/**
 * Herder produsent-API-et mot behandlingens *virkelige* transaksjon (PoC Fase 4e).
 *
 * Bibliotekets egen [efterlatte.prosessering.postgres.OutboxTest] beviser outbox-garantien
 * mot `repo.iEgenTransaksjon`. Her beviser vi det samme mot behandlingens faktiske
 * transaksjonsmekanikk — kotliquery `DataSource.transaction { … }` — via broen
 * [opprettISammeTransaksjon]. Det er *denne* koblingen den ekte outboxen vil bruke når
 * en task skrives i samme transaksjon som behandlings-skrivet.
 *
 * - Commit: forretnings-raden og task-raden committer sammen (begge finnes).
 * - Rollback: kaster forretnings-skrivet, og task-raden ruller tilbake med det (ingen finnes).
 */
@ExtendWith(ProsesseringDatabaseExtension::class)
class OutboxSammeTransaksjonIntegrationTest {
    private val forretningType = strengType("OutboxForretningTest")

    @Test
    fun `task committer sammen med behandlingens kotliquery-transaksjon`(dataSource: DataSource) {
        klargjoer(dataSource)
        val repo = PostgresTaskRepository(dataSource)
        val produsent = StandardTaskProdusent(repo)

        val taskId =
            dataSource.transaction { tx ->
                skrivForretning(tx, "sak-1")
                produsent.opprettISammeTransaksjon(
                    transaksjon = tx,
                    type = forretningType,
                    payload = """{"sak":"sak-1"}""",
                )
            }

        assertEquals(1, antallForretning(dataSource), "Forretnings-raden skal ha committet")
        assertEquals(Status.KLAR, repo.finn(taskId.verdi)?.status, "Task-en skal ha committet sammen med den")
    }

    @Test
    fun `task rulles tilbake sammen med behandlingens kotliquery-transaksjon`(dataSource: DataSource) {
        klargjoer(dataSource)
        val repo = PostgresTaskRepository(dataSource)
        val produsent = StandardTaskProdusent(repo)

        assertThrows<RuntimeException> {
            dataSource.transaction { tx ->
                skrivForretning(tx, "sak-2")
                produsent.opprettISammeTransaksjon(
                    transaksjon = tx,
                    type = forretningType,
                    payload = """{"sak":"sak-2"}""",
                )
                throw RuntimeException("forretnings-skrivet feilet etter at tasken ble lagt til")
            }
        }

        assertEquals(0, antallForretning(dataSource), "Rullet tilbake transaksjon skal ikke etterlate forretnings-rad")
        assertEquals(0, repo.antallMedStatus(Status.KLAR), "Task-en skal ha rullet tilbake sammen med forretnings-skrivet")
    }

    private fun klargjoer(dataSource: DataSource) {
        dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                statement.execute("CREATE TABLE IF NOT EXISTS prosessering.forretning_test (id TEXT PRIMARY KEY)")
                statement.execute("TRUNCATE TABLE prosessering.forretning_test")
                statement.execute("TRUNCATE TABLE prosessering.task")
            }
        }
    }

    private fun skrivForretning(
        tx: TransactionalSession,
        id: String,
    ) {
        tx.run(queryOf("INSERT INTO prosessering.forretning_test (id) VALUES (?)", id).asUpdate)
    }

    private fun antallForretning(dataSource: DataSource): Int =
        dataSource.connection.use { connection ->
            connection.prepareStatement("SELECT count(*) FROM prosessering.forretning_test").use { statement ->
                statement.executeQuery().use { resultSet ->
                    resultSet.next()
                    resultSet.getInt(1)
                }
            }
        }
}
