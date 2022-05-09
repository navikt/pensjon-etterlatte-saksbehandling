package no.nav.etterlatte.utbetaling.grensesnittavstemming

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.etterlatte.utbetaling.TestContainers
import no.nav.etterlatte.utbetaling.config.DataSourceBuilder
import no.nav.etterlatte.utbetaling.common.Tidspunkt
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.junit.jupiter.Container
import java.time.Instant
import java.time.temporal.ChronoUnit
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
            periodeFraOgMed = Tidspunkt(Instant.now().minus(1, ChronoUnit.DAYS)),
            periodeTil = Tidspunkt.now(),
            antallOppdrag = 1,
            opprettet = Tidspunkt.now()
        )

        val antallRaderOppdatert = grensesnittavstemmingDao.opprettAvstemming(grensesnittavstemming)

        assertEquals(1, antallRaderOppdatert)
    }

    @Test
    fun `skal hente nyeste avstemming`() {
        val now = Tidspunkt.now()

        val grensesnittavstemming1 = Grensesnittavstemming(
            opprettet = now,
            periodeFraOgMed = now.minus(1, ChronoUnit.DAYS),
            periodeTil = now,
            antallOppdrag = 1
        )

        val grensesnittavstemming2 = Grensesnittavstemming(
            opprettet = now.minus(1, ChronoUnit.DAYS),
            periodeFraOgMed = now.minus(2, ChronoUnit.DAYS),
            periodeTil = now.minus(1, ChronoUnit.DAYS),
            antallOppdrag = 2
        )

        val grensesnittavstemming3 = Grensesnittavstemming(
            opprettet = now.minus(2, ChronoUnit.DAYS),
            periodeFraOgMed = now.minus(3, ChronoUnit.DAYS),
            periodeTil = now.minus(2, ChronoUnit.DAYS),
            antallOppdrag = 3
        )

        grensesnittavstemmingDao.opprettAvstemming(grensesnittavstemming1)
        grensesnittavstemmingDao.opprettAvstemming(grensesnittavstemming3)
        grensesnittavstemmingDao.opprettAvstemming(grensesnittavstemming2)

        val nyesteAvstemming = grensesnittavstemmingDao.hentSisteAvstemming()

        assertEquals(now, nyesteAvstemming?.opprettet)
        assertEquals(1, nyesteAvstemming?.antallOppdrag)
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