package no.nav.etterlatte.avstemming

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.etterlatte.config.DataSourceBuilder
import no.nav.etterlatte.util.TestContainers
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.junit.jupiter.Container
import java.time.Instant
import java.time.LocalDateTime
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AvstemmingDaoIntegrationTest {

    @Container
    private val postgreSQLContainer = TestContainers.postgreSQLContainer
    private val dataSource: DataSource
    private val avstemmingDao: AvstemmingDao

    init {
        postgreSQLContainer.start()

        DataSourceBuilder(
            jdbcUrl = postgreSQLContainer.jdbcUrl,
            username = postgreSQLContainer.username,
            password = postgreSQLContainer.password
        ).run {
            dataSource = dataSource()
            avstemmingDao = AvstemmingDao(dataSource)
            migrate()
        }
    }

    @Test
    fun `skal opprette avstemming`() {
        val avstemming = Avstemming(
            avstemmingsnokkelTilOgMed = LocalDateTime.now(),
            antallAvstemteOppdrag = 1
        )

        val antallRaderOppdatert = avstemmingDao.opprettAvstemming(avstemming)

        assertEquals(1, antallRaderOppdatert)
    }

    @Test
    fun `skal hente nyeste avstemming`() {
        val now = LocalDateTime.now()

        val avstemming1 = Avstemming(
            opprettet = now,
            avstemmingsnokkelTilOgMed = now,
            antallAvstemteOppdrag = 1
        )

        val avstemming2 = Avstemming(
            opprettet = now.minusDays(1),
            avstemmingsnokkelTilOgMed = now.minusDays(1),
            antallAvstemteOppdrag = 2
        )

        val avstemming3 = Avstemming(
            opprettet = now.minusDays(2),
            avstemmingsnokkelTilOgMed = now.minusDays(2),
            antallAvstemteOppdrag = 3
        )

        avstemmingDao.opprettAvstemming(avstemming1)
        avstemmingDao.opprettAvstemming(avstemming3)
        avstemmingDao.opprettAvstemming(avstemming2)

        val nyesteAvstemming = avstemmingDao.hentSisteAvstemming()

        assertEquals(now, nyesteAvstemming?.opprettet)
        assertEquals(1, nyesteAvstemming?.antallAvstemteOppdrag)

        println(Instant.now())
    }

    @Test
    fun `skal gi null dersom det ikke finnes noen avstemming`() {
        val nyesteAvstemming = avstemmingDao.hentSisteAvstemming()

        assertNull(nyesteAvstemming)
    }

    @AfterEach
    fun afterEach() {
        using(sessionOf(dataSource)) {
            it.run(queryOf("TRUNCATE avstemming").asExecute)
        }
    }

    @AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()
    }

}