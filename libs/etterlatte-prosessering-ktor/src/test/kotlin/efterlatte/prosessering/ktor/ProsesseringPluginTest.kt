package efterlatte.prosessering.ktor

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import efterlatte.prosessering.Status
import efterlatte.prosessering.TaskStep
import efterlatte.prosessering.postgres.PostgresTaskRepository
import efterlatte.prosessering.strengType
import efterlatte.prosessering.taskSteg
import io.ktor.client.request.get
import io.ktor.server.application.install
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.delay
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import javax.sql.DataSource
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Verifiserer at Ktor-pluginen wirer motoren mot applikasjonens livssyklus:
 * når appen starter plukker motoren en KLAR task og kjører den til FULLFØRT, og
 * produsenten er tilgjengelig via [Application.taskProdusent].
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProsesseringPluginTest {
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
    private val skygge = strengType("SoeknadMottakSkygge")

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
    fun `motoren starter med appen og kjorer en task til FULLFORT`() =
        testApplication {
            val mottakSteg: TaskStep<String> = taskSteg(skygge) { }
            lateinit var produsent: efterlatte.prosessering.TaskProdusent
            application {
                install(Prosessering) {
                    repository = repo
                    node = "test"
                    steg = listOf(mottakSteg)
                    reaperPaa = false
                }
                produsent = taskProdusent
            }

            client.get("/")

            val taskId = produsent.opprettFrittstående(type = skygge, payload = """{"sak":1}""")

            ventPaaStatus(taskId.verdi, Status.FULLFØRT)
            assertEquals(Status.FULLFØRT, repo.finn(taskId.verdi)?.status)
        }

    private suspend fun ventPaaStatus(
        id: Long,
        forventet: Status,
    ) {
        val frist = System.currentTimeMillis() + 10.seconds.inWholeMilliseconds
        while (System.currentTimeMillis() < frist) {
            if (repo.finn(id)?.status == forventet) return
            delay(50.milliseconds)
        }
    }
}
