package no.nav.etterlatte.sak

import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SakDaoTest {

    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:14")

    private lateinit var dataSource: DataSource
    private lateinit var sakRepo: SakDao
    private lateinit var tilgangService: TilgangService

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()
        postgreSQLContainer.withUrlParam("user", postgreSQLContainer.username)
        postgreSQLContainer.withUrlParam("password", postgreSQLContainer.password)

        dataSource = DataSourceBuilder.createDataSource(
            jdbcUrl = postgreSQLContainer.jdbcUrl,
            username = postgreSQLContainer.username,
            password = postgreSQLContainer.password
        ).apply { migrate() }
        val connection = dataSource.connection
        sakRepo = SakDao { connection }
        tilgangService = TilgangServiceImpl(
            SakTilgangDao(dataSource),
            emptyMap()
        )
    }

    @Test
    fun `kan opprett sak uten enhet`() {
        val opprettSak = sakRepo.opprettSak("fnr", SakType.BARNEPENSJON)

        Assertions.assertNull(opprettSak.enhet)
    }

    @Test
    fun `kan opprett sak med enhet`() {
        val opprettSak = sakRepo.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.DEFAULT.enhetNr)

        Assertions.assertEquals(Enheter.DEFAULT.enhetNr, opprettSak.enhet)
    }
}