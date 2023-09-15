package no.nav.etterlatte.behandling.generellbehandling

import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.POSTGRES_VERSION
import no.nav.etterlatte.libs.database.migrate
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.util.*
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GenerellBehandlingDaoTest {
    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:$POSTGRES_VERSION")

    private lateinit var dataSource: DataSource
    private lateinit var dao: GenerellBehandlingDao

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()
        postgreSQLContainer.withUrlParam("user", postgreSQLContainer.username)
        postgreSQLContainer.withUrlParam("password", postgreSQLContainer.password)

        dataSource =
            DataSourceBuilder.createDataSource(
                jdbcUrl = postgreSQLContainer.jdbcUrl,
                username = postgreSQLContainer.username,
                password = postgreSQLContainer.password
            ).apply { migrate() }

        val connection = dataSource.connection
        dao = GenerellBehandlingDao { connection }
    }

    @AfterEach
    fun afterEach() {
        dataSource.connection.use {
            it.prepareStatement("TRUNCATE generellbehandling CASCADE;").execute()
        }
    }

    @AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()
    }

    @Test
    fun `Kan opprette og hente en generell behandling utland`() {
        val generellBehandlingUtland =
            GenerellBehandling(
                UUID.randomUUID(),
                1L,
                type = GenerellBehandlingType.ANNEN,
                Innhold.Utland("vlabla", UUID.randomUUID())
            )
        dao.opprettGenerellbehandling(generellBehandlingUtland)
        val hentetGenBehandling = dao.hentGenerellBehandlingMedId(generellBehandlingUtland.id)

        Assertions.assertEquals(generellBehandlingUtland.id, hentetGenBehandling!!.id)
        Assertions.assertEquals(generellBehandlingUtland.type, hentetGenBehandling.type)
        Assertions.assertEquals(generellBehandlingUtland.innhold, hentetGenBehandling.innhold)
    }

    @Test
    fun `Kan hente for sak`() {
        val sakId = 1L
        val generellBehandlingUtland =
            GenerellBehandling(
                UUID.randomUUID(),
                sakId,
                type = GenerellBehandlingType.UTLAND,
                Innhold.Utland("vlabla", UUID.randomUUID())
            )
        val annengenerebehandling =
            GenerellBehandling(
                UUID.randomUUID(),
                1L,
                GenerellBehandlingType.ANNEN,
                Innhold.Annen("innhold")
            )
        dao.opprettGenerellbehandling(generellBehandlingUtland)
        dao.opprettGenerellbehandling(annengenerebehandling)
        val hentetGenBehandling = dao.hentGenerellBehandlingForSak(sakId)
        Assertions.assertEquals(2, hentetGenBehandling.size)
        val generellBehandling = hentetGenBehandling.single { it.type == GenerellBehandlingType.UTLAND }
        Assertions.assertEquals(generellBehandlingUtland.id, generellBehandling.id)
        Assertions.assertEquals(generellBehandlingUtland.type, generellBehandling.type)
        Assertions.assertEquals(generellBehandlingUtland.innhold, generellBehandling.innhold)
    }
}