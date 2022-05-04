package no.nav.etterlatte.grensesnittavstemming

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.etterlatte.TestContainers
import no.nav.etterlatte.config.DataSourceBuilder
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.junit.jupiter.Container
import java.time.LocalDateTime
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GrensesnittavstemmingDaoIntegrationTest {

    @Container
    private val postgreSQLContainer = TestContainers.postgreSQLContainer
    private val dataSource: DataSource
    private val grensesnittavstemmingDao: GrensesnittavstemmingDao

    init {
        postgreSQLContainer.start()

        DataSourceBuilder(
            jdbcUrl = postgreSQLContainer.jdbcUrl,
            username = postgreSQLContainer.username,
            password = postgreSQLContainer.password
        ).run {
            dataSource = dataSource()
            grensesnittavstemmingDao = GrensesnittavstemmingDao(dataSource)
            migrate()
        }
    }

    @Test
    fun `skal opprette avstemming`() {
        val grensesnittavstemming = Grensesnittavstemming(
            fraOgMed = LocalDateTime.now().minusDays(1),
            til = LocalDateTime.now(),
            antallAvstemteOppdrag = 1
        )

        val antallRaderOppdatert = grensesnittavstemmingDao.opprettAvstemming(grensesnittavstemming)

        assertEquals(1, antallRaderOppdatert)
    }

    @Test
    fun `skal hente nyeste avstemming`() {
        val now = LocalDateTime.now()

        val grensesnittavstemming1 = Grensesnittavstemming(
            opprettet = now,
            fraOgMed = LocalDateTime.now().minusDays(1),
            til = now,
            antallAvstemteOppdrag = 1
        )

        val grensesnittavstemming2 = Grensesnittavstemming(
            opprettet = now.minusDays(1),
            fraOgMed = LocalDateTime.now().minusDays(2),
            til = now.minusDays(1),
            antallAvstemteOppdrag = 2
        )

        val grensesnittavstemming3 = Grensesnittavstemming(
            opprettet = now.minusDays(2),
            fraOgMed = LocalDateTime.now().minusDays(3),
            til = now.minusDays(2),
            antallAvstemteOppdrag = 3
        )

        grensesnittavstemmingDao.opprettAvstemming(grensesnittavstemming1)
        grensesnittavstemmingDao.opprettAvstemming(grensesnittavstemming3)
        grensesnittavstemmingDao.opprettAvstemming(grensesnittavstemming2)

        val nyesteAvstemming = grensesnittavstemmingDao.hentSisteAvstemming()

        assertEquals(now, nyesteAvstemming?.opprettet)
        assertEquals(1, nyesteAvstemming?.antallAvstemteOppdrag)
    }

    @Test
    fun `skal gi null dersom det ikke finnes noen avstemming`() {
        val nyesteAvstemming = grensesnittavstemmingDao.hentSisteAvstemming()

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