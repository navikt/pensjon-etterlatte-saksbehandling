package common

import no.nav.etterlatte.common.DatabaseContext
import no.nav.etterlatte.libs.database.DataSourceBuilder
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DatabaseContextTest {
    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:15")
    lateinit var dataSource: DataSource

    @BeforeAll
    fun setUp() {
        postgreSQLContainer.start()
        postgreSQLContainer.withUrlParam("user", postgreSQLContainer.username)
        postgreSQLContainer.withUrlParam("password", postgreSQLContainer.password)

        dataSource = DataSourceBuilder.createDataSource(
            mutableMapOf(
                "DB_HOST" to postgreSQLContainer.host,
                "DB_USERNAME" to postgreSQLContainer.username,
                "DB_PASSWORD" to postgreSQLContainer.password,
                "DB_PORT" to postgreSQLContainer.firstMappedPort.toString(),
                "DB_DATABASE" to postgreSQLContainer.databaseName
            )
        )
    }

    @AfterAll
    fun shutdown() = postgreSQLContainer.stop()

    @Test
    fun `bevarer transaksjonen med gjenbruksflagget aktivert`() {
        val kontekst = DatabaseContext(dataSource)
        kontekst.inTransaction {
            val activeTx1 = kontekst.activeTx()
            kontekst.inTransaction(true) {
                val activeTx2 = kontekst.activeTx()
                assertEquals(activeTx1, activeTx2)
            }
        }
    }

    @Test
    fun `bevarer ikke transaksjonen med gjenbruksflagget deaktivert`() {
        val kontekst = DatabaseContext(dataSource)
        kontekst.inTransaction {
            assertThrows<IllegalStateException> {
                kontekst.inTransaction {}
            }
        }
    }
}