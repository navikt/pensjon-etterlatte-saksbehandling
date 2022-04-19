package no.nav.etterlatte

import no.nav.etterlatte.config.DataSourceBuilder
import no.nav.etterlatte.config.StandardJdbcUrlBuilder
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.util.*
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class VilkaarDaoIntegrationTest {

    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:14")

    private lateinit var dataSource: DataSource
    private lateinit var vilkaarDao: VilkaarDao

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()

        DataSourceBuilder(
            jdbcUrlBuilder = StandardJdbcUrlBuilder(postgreSQLContainer.jdbcUrl),
            databaseUsername = postgreSQLContainer.username,
            databasePassword = postgreSQLContainer.password
        ).also {
            dataSource = it.dataSource()
            vilkaarDao = VilkaarDaoJdbc(dataSource)

            // TODO ikke optimalt at man må kopiere inn skjema fra annen app for å få testet dette
            migrate(dataSource)
        }
    }

    fun migrate(dataSource: DataSource) =
        Flyway.configure()
            .dataSource(dataSource)
            .load()
            .migrate()

    @AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()
    }

    @Test
    fun `hent vilkaar`() {
        // TODO legg til en vilkaarsvurdering for å teste uthenting

        val behandlingId = UUID.randomUUID().toString()

        val vilkaarResultat = vilkaarDao.hentVilkaarResultat(behandlingId)

        assertNull(vilkaarResultat)
    }

}