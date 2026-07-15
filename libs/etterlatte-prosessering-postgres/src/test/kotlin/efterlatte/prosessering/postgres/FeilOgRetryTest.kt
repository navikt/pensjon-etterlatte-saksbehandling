package efterlatte.prosessering.postgres

import efterlatte.prosessering.ProcessingEngine
import efterlatte.prosessering.StandardTaskProdusent
import efterlatte.prosessering.Status
import efterlatte.prosessering.Stoppaarsak
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
import kotlin.time.Duration.Companion.milliseconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FeilOgRetryTest {
    private val container: PostgreSQLContainer<*> = TestStotte.startPostgres()
    private val dataSource: DataSource = TestStotte.datasource(container, poolStorrelse = 10)
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
    fun `task som alltid feiler havner i STOPPET etter maxAntallFeil og retries underveis`() =
        runBlocking {
            val maxAntallFeil = 3
            val alltidFeil = strengType("AlltidFeil")
            val antallForsok = AtomicInteger(0)
            val taskId = produsent.opprettFrittstående(type = alltidFeil, payload = "{}").verdi

            val engine =
                ProcessingEngine(
                    repo = repo,
                    produsent = produsent,
                    steg =
                        listOf(
                            taskSteg(alltidFeil) {
                                antallForsok.incrementAndGet()
                                throw IllegalStateException("simulert feil")
                            },
                        ),
                    node = "pod-feil",
                    batchStorrelse = 5,
                    maxSamtidighet = 2,
                    maxAntallFeil = maxAntallFeil,
                    backoff = { 50.milliseconds },
                )

            engine.start()

            withTimeout(30_000) {
                while (repo.finn(taskId)?.status != Status.STOPPET) {
                    delay(25)
                }
            }

            engine.stop()

            val task = repo.finn(taskId)!!

            println("=== FEIL/RETRY-RESULTAT ===")
            println("Sluttstatus:      ${task.status}")
            println("Stoppårsak:       ${task.stoppaarsak}")
            println("antallFeil i DB:  ${task.antallFeil}")
            println("Faktiske forsøk:  ${antallForsok.get()}")
            println("===========================")

            assertEquals(Status.STOPPET, task.status, "Skal ende i STOPPET")
            assertEquals(Stoppaarsak.FEIL, task.stoppaarsak, "Stoppårsak skal være FEIL")
            assertEquals(maxAntallFeil, task.antallFeil, "antallFeil skal være == maxAntallFeil")
            assertEquals(maxAntallFeil, antallForsok.get(), "Handleren skal ha kjørt maxAntallFeil ganger")
        }

    @Test
    fun `task som feiler en gang og deretter lykkes ender FULLFØRT via KLAR`() =
        runBlocking {
            val feilEnGang = strengType("FeilEnGang")
            val forsokPerTask = ConcurrentHashMap<Long, AtomicInteger>()
            val observerteStatuser = mutableListOf<Status>()
            val taskId = produsent.opprettFrittstående(type = feilEnGang, payload = "{}").verdi

            val engine =
                ProcessingEngine(
                    repo = repo,
                    produsent = produsent,
                    steg =
                        listOf(
                            taskSteg(feilEnGang) { kontekst ->
                                val forsok = forsokPerTask.computeIfAbsent(kontekst.task.id) { AtomicInteger(0) }.incrementAndGet()
                                if (forsok == 1) throw IllegalStateException("feiler første gang")
                                // andre gang: lykkes
                            },
                        ),
                    node = "pod-retry",
                    batchStorrelse = 5,
                    maxSamtidighet = 2,
                    maxAntallFeil = 3,
                    backoff = { 50.milliseconds },
                )

            engine.start()

            // observer at tasken er innom KLAR før den blir FULLFØRT
            withTimeout(30_000) {
                while (repo.finn(taskId)?.status != Status.FULLFØRT) {
                    repo.finn(taskId)?.status?.let { observerteStatuser.add(it) }
                    delay(20)
                }
            }

            engine.stop()

            val task = repo.finn(taskId)!!

            println("=== RETRY-SUKSESS-RESULTAT ===")
            println("Sluttstatus:     ${task.status}")
            println("antallFeil i DB: ${task.antallFeil}")
            println("Forsøk totalt:   ${forsokPerTask[taskId]?.get()}")
            println("Innom KLAR underveis: ${observerteStatuser.contains(Status.KLAR)}")
            println("==============================")

            assertEquals(Status.FULLFØRT, task.status, "Skal ende FULLFØRT etter retry")
            assertEquals(1, task.antallFeil, "antallFeil skal være 1 etter ett feilet forsøk")
            assertEquals(2, forsokPerTask[taskId]?.get(), "Handleren skal ha kjørt 2 ganger (feil + suksess)")
            assertTrue(
                observerteStatuser.contains(Status.KLAR),
                "Tasken skal ha vært innom KLAR på vei mot retry",
            )
        }
}
