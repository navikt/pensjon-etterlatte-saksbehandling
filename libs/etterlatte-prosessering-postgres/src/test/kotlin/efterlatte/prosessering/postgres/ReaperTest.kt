package efterlatte.prosessering.postgres

import efterlatte.prosessering.Reaper
import efterlatte.prosessering.Status
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import java.time.Instant
import javax.sql.DataSource
import kotlin.time.Duration.Companion.minutes

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReaperTest {
    private val container: PostgreSQLContainer<*> = TestStotte.startPostgres()
    private val dataSource: DataSource = TestStotte.datasource(container, poolStorrelse = 5)
    private val repo = PostgresTaskRepository(dataSource)

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
    fun `hengende KJOERER-task eldre enn timeout settes tilbake til KLAR`() =
        runBlocking {
            val taskId = repo.insertFrittstaaende(type = "Hengende", payload = "{}", triggerTid = Instant.now())
            repo.claimBatch(limit = 10)
            assertEquals(Status.KJØRER, repo.finn(taskId)!!.status, "Skal være plukket til KJØRER")

            val reaper =
                Reaper(
                    repo = repo,
                    hengendeTimeout = 5.minutes,
                    naa = { Instant.now().plusSeconds(600) },
                )

            val antall = reaper.gjenopprettEnGang()

            val task = repo.finn(taskId)!!
            assertEquals(1, antall, "Én hengende task skal gjenopprettes")
            assertEquals(Status.KLAR, task.status, "Skal være tilbake i KLAR")
            assertEquals(null, task.plukketTid, "plukket_tid skal nullstilles ved gjenoppretting")
        }

    @Test
    fun `nettopp plukket task innenfor timeout roeres ikke`() =
        runBlocking {
            val taskId = repo.insertFrittstaaende(type = "FerskKjoerer", payload = "{}", triggerTid = Instant.now())
            repo.claimBatch(limit = 10)
            assertEquals(Status.KJØRER, repo.finn(taskId)!!.status, "Skal være plukket til KJØRER")

            val reaper = Reaper(repo = repo, hengendeTimeout = 5.minutes)

            val antall = reaper.gjenopprettEnGang()

            assertEquals(0, antall, "Ingen tasker skal gjenopprettes")
            assertEquals(Status.KJØRER, repo.finn(taskId)!!.status, "Fersk task skal fortsatt kjøre")
        }
}
