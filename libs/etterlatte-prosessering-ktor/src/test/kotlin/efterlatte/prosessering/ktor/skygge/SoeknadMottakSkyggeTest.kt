package efterlatte.prosessering.ktor.skygge

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import efterlatte.prosessering.Status
import efterlatte.prosessering.Stoppaarsak
import efterlatte.prosessering.TaskProdusent
import efterlatte.prosessering.ktor.Prosessering
import efterlatte.prosessering.ktor.taskProdusent
import efterlatte.prosessering.postgres.PostgresTaskRepository
import io.ktor.client.request.get
import io.ktor.server.application.install
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.delay
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import java.util.concurrent.ConcurrentLinkedQueue
import javax.sql.DataSource
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Fase 3 ende-til-ende: skyggekjøringen tar imot en søknad som en task, validerer og
 * «logger» mottaket (fanget via en observatør) → FULLFØRT, og en ugyldig søknad
 * (feil fnr) prøves på nytt og havner til slutt i STOPPET (FEIL) — reliable,
 * retryable og observerbar behandling, uten sideeffekter.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SoeknadMottakSkyggeTest {
    private val container: PostgreSQLContainer<*> =
        PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("prosessering")
            .withUsername("poc")
            .withPassword("poc")
            .also { it.start() }

    private val dataSource: DataSource =
        HikariDataSource(
            HikariConfig().apply {
                jdbcUrl = container.jdbcUrl
                username = container.username
                password = container.password
                maximumPoolSize = 5
            },
        )

    private val repo = PostgresTaskRepository(dataSource)

    init {
        val skjema = this::class.java.getResource("/schema.sql")!!.readText()
        dataSource.connection.use { connection ->
            connection.createStatement().use { it.execute(skjema) }
        }
    }

    @BeforeEach
    fun tomTabell() {
        dataSource.connection.use { connection ->
            connection.createStatement().use { it.execute("TRUNCATE TABLE prosessering.task") }
        }
    }

    @AfterAll
    fun ryddOpp() {
        (dataSource as? AutoCloseable)?.close()
        container.stop()
    }

    @Test
    fun `gyldig soknad blir en task som fullfores og observeres`() =
        testApplication {
            val mottatt = ConcurrentLinkedQueue<SoeknadMottakPayload>()
            lateinit var produsent: TaskProdusent
            application {
                install(Prosessering) {
                    repository = repo
                    node = "test"
                    steg = listOf(soeknadMottakSkyggeSteg { mottatt.add(it) })
                    reaperPaa = false
                }
                produsent = taskProdusent
            }
            client.get("/")

            val payload =
                SoeknadMottakPayload(
                    soeknadId = "soknad-123",
                    sakType = SakType.OMSTILLINGSSTOENAD,
                    fnrSoeker = "12345678901",
                )
            val taskId = produsent.opprettFrittstående(type = soeknadMottakSkyggeType, payload = payload)

            ventPaaStatus(taskId.verdi, Status.FULLFØRT)

            assertEquals(Status.FULLFØRT, repo.finn(taskId.verdi)?.status)
            assertEquals(listOf(payload), mottatt.toList())
        }

    @Test
    fun `ugyldig fnr provdes pa nytt og ender i STOPPET`() =
        testApplication {
            val mottatt = ConcurrentLinkedQueue<SoeknadMottakPayload>()
            lateinit var produsent: TaskProdusent
            application {
                install(Prosessering) {
                    repository = repo
                    node = "test"
                    steg = listOf(soeknadMottakSkyggeSteg { mottatt.add(it) })
                    reaperPaa = false
                }
                produsent = taskProdusent
            }
            client.get("/")

            val ugyldig =
                SoeknadMottakPayload(
                    soeknadId = "soknad-456",
                    sakType = SakType.BARNEPENSJON,
                    fnrSoeker = "123",
                )
            val taskId = produsent.opprettFrittstående(type = soeknadMottakSkyggeType, payload = ugyldig)

            ventPaaStatus(taskId.verdi, Status.STOPPET)

            val task = repo.finn(taskId.verdi)
            assertEquals(Status.STOPPET, task?.status)
            assertEquals(Stoppaarsak.FEIL, task?.stoppaarsak)
            assertTrue(mottatt.isEmpty(), "steget skal aldri observere en ugyldig søknad")
        }

    private suspend fun ventPaaStatus(
        id: Long,
        forventet: Status,
    ) {
        val frist = System.currentTimeMillis() + 15.seconds.inWholeMilliseconds
        while (System.currentTimeMillis() < frist) {
            if (repo.finn(id)?.status == forventet) return
            delay(50.milliseconds)
        }
    }
}
