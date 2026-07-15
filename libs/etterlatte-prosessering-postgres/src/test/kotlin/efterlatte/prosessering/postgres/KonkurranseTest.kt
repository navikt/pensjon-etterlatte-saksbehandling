package efterlatte.prosessering.postgres

import efterlatte.prosessering.ProcessingEngine
import efterlatte.prosessering.StandardTaskProdusent
import efterlatte.prosessering.Status
import efterlatte.prosessering.strengType
import efterlatte.prosessering.taskSteg
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KonkurranseTest {
    private val container: PostgreSQLContainer<*> = TestStotte.startPostgres()
    private val dataSource: DataSource = TestStotte.datasource(container, poolStorrelse = 40)
    private val repo = PostgresTaskRepository(dataSource)
    private val produsent = StandardTaskProdusent(repo)

    init {
        TestStotte.anvendSkjema(dataSource)
    }

    @AfterAll
    fun ryddOpp() {
        (dataSource as? AutoCloseable)?.close()
        container.stop()
    }

    @Test
    fun `ingen dobbel-eksekvering med mange parallelle workers`() =
        runBlocking {
            val antallTasks = 1000
            val antallEngines = 4
            val sendVedtaksbrev = strengType("SendVedtaksbrev")

            repeat(antallTasks) { i ->
                produsent.opprettFrittstående(type = sendVedtaksbrev, payload = """{"nr":$i}""")
            }

            // Tråd-sikker fasit: hver task-id skal kun forekomme én gang.
            val eksekveringerPerTask = ConcurrentHashMap<Long, AtomicInteger>()
            val totaltAntall = AtomicInteger(0)

            val engines =
                (1..antallEngines).map { nr ->
                    val steg =
                        taskSteg(sendVedtaksbrev) { kontekst ->
                            // DB-en håndhever også via UNIQUE-constraint: en dobbel-kjøring kaster her.
                            TestStotte.loggEksekvering(dataSource = dataSource, taskId = kontekst.task.id, node = "pod-$nr")
                            eksekveringerPerTask.computeIfAbsent(kontekst.task.id) { AtomicInteger(0) }.incrementAndGet()
                            totaltAntall.incrementAndGet()
                        }
                    ProcessingEngine(
                        repo = repo,
                        produsent = produsent,
                        steg = listOf(steg),
                        node = "pod-$nr",
                        batchStorrelse = 20,
                        maxSamtidighet = 8,
                    )
                }

            engines.forEach { it.start() }

            withTimeout(60_000) {
                while (repo.antallMedStatus(Status.FULLFØRT) < antallTasks) {
                    delay(50)
                }
            }

            engines.forEach { it.stop() }

            val fullført = repo.antallMedStatus(Status.FULLFØRT)
            val dobbeltKjorte = eksekveringerPerTask.filterValues { it.get() > 1 }.keys
            val eksekveringerIDb = TestStotte.antallEksekveringer(dataSource)

            println("=== KONKURRANSE-RESULTAT ===")
            println("Tasks satt opp:         $antallTasks")
            println("Engines (pods):         $antallEngines")
            println("Tasks i status FULLFØRT: $fullført")
            println("Totalt eksekveringer:   ${totaltAntall.get()}")
            println("Unike eksekveringer:    ${eksekveringerPerTask.size}")
            println("Eksekveringer i DB:     $eksekveringerIDb")
            println("Dobbel-eksekverte:      ${dobbeltKjorte.size} $dobbeltKjorte")
            println("============================")

            assertEquals(antallTasks, fullført, "Alle tasks skal være FULLFØRT")
            assertEquals(antallTasks, totaltAntall.get(), "Totalt antall eksekveringer skal være == N")
            assertEquals(antallTasks, eksekveringerPerTask.size, "Antall unike task-id skal være == N")
            assertEquals(antallTasks, eksekveringerIDb, "Antall rader i execution_log skal være == N")
            assertTrue(dobbeltKjorte.isEmpty(), "Ingen task skal være eksekvert mer enn én gang")
        }
}
