package efterlatte.prosessering.postgres

import efterlatte.prosessering.StandardTaskProdusent
import efterlatte.prosessering.Status
import efterlatte.prosessering.Task
import efterlatte.prosessering.TaskKontekst
import efterlatte.prosessering.strengType
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.testcontainers.containers.PostgreSQLContainer
import java.sql.Connection
import java.time.Instant
import javax.sql.DataSource

/**
 * Herder de to delene av den offentlige kontrakten som resten av PoC-en ennå ikke kjører, og som
 * uttrekket til eget repo eksponerer for konsumenter:
 *
 *  1. **Ekte forretnings-skriv gjennom stegets [TaskKontekst.transaksjon]** — et [TaskStep] skal
 *     kunne gjøre sitt eget DB-skriv på *samme* transaksjon som `FULLFØRT`-oppdateringen.
 *  2. **[TaskKontekst.opprettNesteTask]** — atomiske kjeder: neste task legges i kø på samme
 *     transaksjon, så «dette steget fullført» og «neste task opprettet» committer eller ruller
 *     tilbake sammen.
 *
 * Begge speiler [OutboxTest] sin garanti, men fra *stegets* side (nedstrøms), ikke produsentens.
 * Bevis begge veier: commit ⇒ begge finnes; feil ⇒ ingen av delene.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KjedeOgStegTransaksjonTest {
    private val container: PostgreSQLContainer<*> = TestStotte.startPostgres()
    private val dataSource: DataSource = TestStotte.datasource(container, poolStorrelse = 5)
    private val repo = PostgresTaskRepository(dataSource)
    private val produsent = StandardTaskProdusent(repo)
    private val nesteSteg = strengType("NesteSteg")

    init {
        TestStotte.anvendSkjema(dataSource)
        dataSource.connection.use { connection ->
            connection.createStatement().use {
                it.execute("CREATE TABLE IF NOT EXISTS prosessering.forretning (behandling_id TEXT PRIMARY KEY)")
            }
        }
    }

    @BeforeEach
    fun tomTabeller() {
        dataSource.connection.use { connection ->
            connection.createStatement().use {
                it.execute("TRUNCATE TABLE prosessering.task, prosessering.forretning")
            }
        }
    }

    @AfterAll
    fun ryddOpp() {
        (dataSource as? AutoCloseable)?.close()
        container.stop()
    }

    @Test
    fun `forretnings-skriv og opprettNesteTask committer sammen med stegets transaksjon`() {
        repo.iEgenTransaksjon { transaksjon ->
            skrivForretning(transaksjon, behandlingId = "beh-1")
            kontekstFor(transaksjon).opprettNesteTask(type = nesteSteg, payload = "neste-1")
        }

        assertEquals(1, antallForretning("beh-1"), "Forretnings-raden skal være committet")
        assertEquals(1, repo.antallMedStatus(Status.KLAR), "Den kjedede tasken skal være committet som KLAR")
    }

    @Test
    fun `forretnings-skriv og opprettNesteTask rulles tilbake når steget feiler`() {
        assertThrows<RuntimeException> {
            repo.iEgenTransaksjon { transaksjon ->
                skrivForretning(transaksjon, behandlingId = "beh-2")
                kontekstFor(transaksjon).opprettNesteTask(type = nesteSteg, payload = "neste-2")
                throw RuntimeException("steget feilet etter forretnings-skriv og kjeding")
            }
        }

        assertEquals(0, antallForretning("beh-2"), "Forretnings-raden skal ha rullet tilbake")
        assertEquals(0, repo.antallMedStatus(Status.KLAR), "Den kjedede tasken skal ha rullet tilbake")
    }

    private fun kontekstFor(transaksjon: efterlatte.prosessering.Transaksjon): TaskKontekst<String> =
        TaskKontekst(
            task = enTask,
            payload = "payload",
            transaksjon = transaksjon,
            produsent = produsent,
        )

    private fun skrivForretning(
        transaksjon: efterlatte.prosessering.Transaksjon,
        behandlingId: String,
    ) {
        val connection = (transaksjon as JdbcTransaksjon).connection
        connection.prepareStatement("INSERT INTO prosessering.forretning (behandling_id) VALUES (?)").use {
            it.setString(1, behandlingId)
            it.executeUpdate()
        }
    }

    private fun antallForretning(behandlingId: String): Int =
        dataSource.connection.use { connection: Connection ->
            connection.prepareStatement("SELECT count(*) FROM prosessering.forretning WHERE behandling_id = ?").use {
                it.setString(1, behandlingId)
                it.executeQuery().use { resultSet ->
                    resultSet.next()
                    resultSet.getInt(1)
                }
            }
        }

    private val enTask =
        Task(
            id = 1,
            type = "DetteSteget",
            status = Status.KJØRER,
            payload = "payload",
            triggerTid = Instant.now(),
            opprettetTid = Instant.now(),
            plukketTid = Instant.now(),
            antallFeil = 0,
            stoppaarsak = null,
            versjon = 1,
        )
}
