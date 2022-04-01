import no.nav.etterlatte.attestering.AttestasjonDao
import no.nav.etterlatte.config.DataSourceBuilder
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import javax.sql.DataSource


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AttestasjonDaoTest {

    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:14")

    private lateinit var dataSource: DataSource
    private lateinit var attestasjonDao: AttestasjonDao

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()

        DataSourceBuilder(
            jdbcUrl = postgreSQLContainer.jdbcUrl,
            username = postgreSQLContainer.username,
            password = postgreSQLContainer.password
        ).let {
            dataSource = it.dataSource()
            attestasjonDao = AttestasjonDao { dataSource.connection }
            it.migrate()
        }
    }

    @AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()
    }

    @AfterEach
    fun afterEach() {
        cleanDatabase()
    }

    private fun cleanDatabase() {
        dataSource.connection.use {
            it.prepareStatement("DELETE FROM attestasjon WHERE vedtak_id IS NOT NULL")
                .apply { execute() }
        }
    }

    @Test
    fun `test`() {
        val vedtak = vedtak()
        val attestasjon = attestasjon()

        attestasjonDao.opprettAttestertVedtak(vedtak, attestasjon)

    }

}
