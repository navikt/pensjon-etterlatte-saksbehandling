package no.nav.etterlatte.prosessering

import efterlatte.prosessering.StandardTaskProdusent
import efterlatte.prosessering.Status
import efterlatte.prosessering.postgres.PostgresTaskRepository
import efterlatte.prosessering.strengType
import no.nav.etterlatte.Self
import no.nav.etterlatte.databaseContext
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.nyKontekstMedBrukerOgDatabase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import javax.sql.DataSource

/**
 * Herder produsent-API-et mot behandlingens **tråd-lokale** transaksjon (PoC Fase 4e, Steg 2a).
 *
 * Der [OutboxSammeTransaksjonIntegrationTest] beviser broen mot en kotliquery-`TransactionalSession`,
 * beviser denne broen mot behandlingens faktiske `inTransaction { … }` (Kontekst/DatabaseContext).
 * Det er *denne* koblingen behandlingsopprettelsen vil bruke: task-en køes via
 * [opprettPaaAktivBehandlingstransaksjon] inne i den samme `inTransaction` som skriver behandling-raden.
 *
 * - Commit: forretnings-raden og task-raden committer sammen (begge finnes).
 * - Rollback: forretnings-skrivet kaster, og task-raden ruller tilbake med det (ingen finnes).
 */
@ExtendWith(ProsesseringDatabaseExtension::class)
class EkteOutboxBehandlingstransaksjonIntegrationTest {
    private val forretningType = strengType("EkteOutboxForretningTest")

    @Test
    fun `task committer sammen med behandlingens inTransaction`(dataSource: DataSource) {
        klargjoer(dataSource)
        val repo = PostgresTaskRepository(dataSource)
        val produsent = StandardTaskProdusent(repo)
        nyKontekstMedBrukerOgDatabase(Self("test"), dataSource)

        val taskId =
            inTransaction {
                skrivForretning("sak-1")
                produsent.opprettPaaAktivBehandlingstransaksjon(
                    type = forretningType,
                    payload = """{"sak":"sak-1"}""",
                )
            }

        assertEquals(1, antallForretning(dataSource), "Forretnings-raden skal ha committet")
        assertEquals(Status.KLAR, repo.finn(taskId.verdi)?.status, "Task-en skal ha committet sammen med den")
    }

    @Test
    fun `task rulles tilbake sammen med behandlingens inTransaction`(dataSource: DataSource) {
        klargjoer(dataSource)
        val repo = PostgresTaskRepository(dataSource)
        val produsent = StandardTaskProdusent(repo)
        nyKontekstMedBrukerOgDatabase(Self("test"), dataSource)

        assertThrows<RuntimeException> {
            inTransaction {
                skrivForretning("sak-2")
                produsent.opprettPaaAktivBehandlingstransaksjon(
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

    private fun skrivForretning(id: String) {
        databaseContext().activeTx().prepareStatement("INSERT INTO prosessering.forretning_test (id) VALUES (?)").use {
            it.setString(1, id)
            it.executeUpdate()
        }
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
