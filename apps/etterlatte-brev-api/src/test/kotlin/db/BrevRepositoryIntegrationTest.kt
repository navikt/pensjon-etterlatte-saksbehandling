package db

import no.nav.etterlatte.DataSourceBuilder
import no.nav.etterlatte.db.BrevRepository
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import javax.sql.DataSource

@Disabled
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BrevRepositoryIntegrationTest {
/*
    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:14")

    private lateinit var db: BrevRepository
    private lateinit var dataSource: DataSource

    private val connection get() = dataSource.connection

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()
        postgreSQLContainer.withUrlParam("user", postgreSQLContainer.username)
        postgreSQLContainer.withUrlParam("password", postgreSQLContainer.password)

        val dsb = DataSourceBuilder(mapOf("DB_JDBC_URL" to postgreSQLContainer.jdbcUrl))
        dataSource = dsb.dataSource

        dsb.migrate()
        db = BrevRepository.using(dataSource)
    }

    @AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()
    }

    @AfterEach
    fun resetTablesAfterEachTest() {
        connection.use {
            it.prepareStatement("TRUNCATE brev RESTART IDENTITY CASCADE;").execute()
        }
    }

    @Test
    fun test() {
        val vedtakId: Long = 1
        val pdfBytes = "Hello World!".toByteArray()

        assertNull(db.hentBrev(vedtakId))

        val nyttBrev = db.opprettBrev(vedtakId, pdfBytes)

        val hentetBrev = db.hentBrev(nyttBrev.vedtakId)!!

        assertEquals(nyttBrev.id, hentetBrev.id)
        assertEquals(vedtakId, hentetBrev.vedtakId)

        assertEquals(String(pdfBytes), String(hentetBrev.data))
    }
*/
}
