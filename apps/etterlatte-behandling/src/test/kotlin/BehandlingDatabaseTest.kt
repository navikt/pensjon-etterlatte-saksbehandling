package soeknad

import no.nav.etterlatte.DataSourceBuilder
import no.nav.etterlatte.behandling.*
import no.nav.etterlatte.sak.SakDao
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.util.*
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BehandlingDaoIntegrationTest {
    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:12")
    

    private lateinit var dataSource: DataSource

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()
        postgreSQLContainer.withUrlParam("user", postgreSQLContainer.username)
        postgreSQLContainer.withUrlParam("password", postgreSQLContainer.password)

        val dsb = DataSourceBuilder(mapOf("DB_JDBC_URL" to postgreSQLContainer.jdbcUrl))
        dataSource = dsb.dataSource

        dsb.migrate()
    }

    @AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()
    }

    @Test
    fun `Lagring og uthenting av kladd fungerer som forventet`() {
        val connection = dataSource.connection
        val sakrepo = SakDao{connection}
        val db = BehandlingDao { connection }
        val nybehandling = Behandling(UUID.randomUUID(), sakrepo.opprettSak("", "BP").id, emptyList(), null, null, false)
        db.opprett(nybehandling)
        println(db.hent(nybehandling.id))

        db.lagreBeregning(nybehandling)
        println(db.hent(nybehandling.id))
        //db.nyOpplysning(nybehandling.id, Opplysning(UUID.randomUUID(), Opplysning.Privatperson("fnr", Instant.now()), "lol", objectMapper.createObjectNode(), objectMapper.createObjectNode()))
        println(db.hent(nybehandling.id))

        connection.close()
    }

}
