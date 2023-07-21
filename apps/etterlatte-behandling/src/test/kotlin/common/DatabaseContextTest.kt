package no.nav.etterlatte.common

import io.mockk.mockk
import no.nav.etterlatte.Context
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.sak.SakDao
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DatabaseContextTest {
    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:15")
    private val user = mockk<SaksbehandlerMedEnheterOgRoller>()

    @BeforeAll
    fun setUp() {
        postgreSQLContainer.start()
        postgreSQLContainer.withUrlParam("user", postgreSQLContainer.username)
        postgreSQLContainer.withUrlParam("password", postgreSQLContainer.password)

        Kontekst.set(
            Context(
                user,
                DatabaseContext(
                    DataSourceBuilder.createDataSource(
                        mutableMapOf(
                            "DB_HOST" to postgreSQLContainer.host,
                            "DB_USERNAME" to postgreSQLContainer.username,
                            "DB_PASSWORD" to postgreSQLContainer.password,
                            "DB_PORT" to postgreSQLContainer.firstMappedPort.toString(),
                            "DB_DATABASE" to postgreSQLContainer.databaseName
                        )
                    ).also { it.migrate() }
                )
            )
        )
    }

    @AfterAll
    fun shutdown() = postgreSQLContainer.stop()

    @Test
    fun `bevarer transaksjonen med gjenbruksflagget aktivert`() {
        val dao = SakDao { activeTransaction() }
        inTransaction {
            val activeTx1 = activeTransaction()
            dao.hentSaker()
            inTransaction(true) {
                val activeTx2 = activeTransaction()
                dao.hentSaker()
                assertEquals(activeTx1, activeTx2)
            }
        }
    }

    @Test
    fun `bevarer ikke transaksjonen med gjenbruksflagget deaktivert`() {
        val dao = SakDao { activeTransaction() }
        inTransaction {
            dao.hentSaker()
            assertThrows<IllegalStateException> {
                inTransaction {
                    dao.hentSaker()
                }
            }
        }
    }

    private fun activeTransaction() = Kontekst.get().databasecontxt.activeTx()
}