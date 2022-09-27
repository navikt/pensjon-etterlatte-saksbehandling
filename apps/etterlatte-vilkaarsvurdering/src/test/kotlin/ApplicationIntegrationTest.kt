package no.nav.etterlatte

import io.mockk.spyk
import no.nav.etterlatte.testsupport.TestRapid
import no.nav.etterlatte.vilkaarsvurdering.config.ApplicationContext
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApplicationIntegrationTest {

    // @Container
    // private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:14")

    private lateinit var dataSource: DataSource
    private val rapidsConnection: TestRapid = spyk(TestRapid())

    @BeforeAll
    fun beforeAll() {
        // postgreSQLContainer.start()

        /*
        val applicationProperties = ApplicationProperties(
            dbName = postgreSQLContainer.databaseName,
            dbHost = postgreSQLContainer.host,
            dbPort = postgreSQLContainer.firstMappedPort,
            dbUsername = postgreSQLContainer.username,
            dbPassword = postgreSQLContainer.password
        )
         */

        ApplicationContext().also {
            rapidApplication(it, rapidsConnection).start()
        }
    }

    @Test
    fun `en test`() {
    }

    /*
    @AfterEach
    fun afterEach() {
        using(sessionOf(dataSource)) {
            // it.run(queryOf("TRUNCATE vilkaarsvurdering CASCADE").asExecute)
        }
    }*/

    @AfterAll
    fun afterAll() {
        rapidsConnection.stop()
        // postgreSQLContainer.stop()
    }
}