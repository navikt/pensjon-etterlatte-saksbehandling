package efterlatte.prosessering.postgres

import efterlatte.prosessering.StandardTaskProdusent
import efterlatte.prosessering.Status
import efterlatte.prosessering.strengType
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.testcontainers.containers.PostgreSQLContainer
import javax.sql.DataSource

/**
 * Beviser outbox-garantien: `opprett` skriver på kallerens transaksjon, så
 * task-raden lever og dør sammen med forretnings-skrivet i samme transaksjon.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OutboxTest {
    private val container: PostgreSQLContainer<*> = TestStotte.startPostgres()
    private val dataSource: DataSource = TestStotte.datasource(container, poolStorrelse = 5)
    private val repo = PostgresTaskRepository(dataSource)
    private val produsent = StandardTaskProdusent(repo)
    private val vedtaksbrev = strengType("SendVedtaksbrev")

    init {
        TestStotte.anvendSkjema(dataSource)
    }

    @BeforeEach
    fun tomTabell() {
        dataSource.connection.use { connection ->
            connection.createStatement().use { it.execute("TRUNCATE TABLE task") }
        }
    }

    @AfterAll
    fun ryddOpp() {
        (dataSource as? AutoCloseable)?.close()
        container.stop()
    }

    @Test
    fun `task rulles tilbake sammen med transaksjonen som opprettet den`() {
        assertThrows<RuntimeException> {
            repo.iEgenTransaksjon { transaksjon ->
                produsent.opprett(transaksjon = transaksjon, type = vedtaksbrev, payload = """{"vedtak":1}""")
                throw RuntimeException("forretnings-skrivet feilet etter at tasken ble lagt til")
            }
        }

        assertEquals(0, repo.antallMedStatus(Status.KLAR), "Rullet tilbake transaksjon skal ikke etterlate task")
    }

    @Test
    fun `task committer sammen med transaksjonen som opprettet den`() {
        val taskId =
            repo.iEgenTransaksjon { transaksjon ->
                produsent.opprett(transaksjon = transaksjon, type = vedtaksbrev, payload = """{"vedtak":2}""")
            }

        assertEquals(1, repo.antallMedStatus(Status.KLAR), "Committet transaksjon skal etterlate én KLAR task")
        assertEquals(Status.KLAR, repo.finn(taskId.verdi)?.status)
    }
}
