package no.nav.etterlatte.utbetaling.grensesnittavstemming

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.utbetaling.TestContainers
import no.nav.etterlatte.utbetaling.config.DataSourceBuilder
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Saktype
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
    fun `skal opprette avstemming for Barnepensjon`() {
        val grensesnittavstemming = Grensesnittavstemming(
            periode = Avstemmingsperiode(
                fraOgMed = Tidspunkt(Instant.now().minus(1, ChronoUnit.DAYS)),
                til = Tidspunkt.now()
            ),
            antallOppdrag = 1,
            opprettet = Tidspunkt.now(),
            avstemmingsdata = "",
            saktype = Saktype.BARNEPENSJON
        )

        val antallRaderOppdatert = avstemmingDao.opprettGrensesnittavstemming(grensesnittavstemming)

        assertEquals(1, antallRaderOppdatert)
    }

    @Test
    fun `skal hente nyeste avstemming`() {
        val now = Tidspunkt.now()

        val grensesnittavstemming1 = Grensesnittavstemming(
            opprettet = now,
            periode = Avstemmingsperiode(
                fraOgMed = now.minus(1, ChronoUnit.DAYS),
                til = now
            ),
            antallOppdrag = 1,
            avstemmingsdata = "",
            saktype = Saktype.BARNEPENSJON
        )

        val grensesnittavstemming2 = Grensesnittavstemming(
            opprettet = now.minus(1, ChronoUnit.DAYS),
            periode = Avstemmingsperiode(
                fraOgMed = now.minus(2, ChronoUnit.DAYS),
                til = now.minus(1, ChronoUnit.DAYS)
            ),
            antallOppdrag = 2,
            avstemmingsdata = "",
            saktype = Saktype.BARNEPENSJON
        )

        val grensesnittavstemming3 = Grensesnittavstemming(
            opprettet = now.minus(2, ChronoUnit.DAYS),
            periode = Avstemmingsperiode(
                fraOgMed = now.minus(3, ChronoUnit.DAYS),
                til = now.minus(2, ChronoUnit.DAYS)
            ),
            antallOppdrag = 3,
            avstemmingsdata = "",
            saktype = Saktype.BARNEPENSJON
        )

        avstemmingDao.opprettGrensesnittavstemming(grensesnittavstemming1)
        avstemmingDao.opprettGrensesnittavstemming(grensesnittavstemming3)
        avstemmingDao.opprettGrensesnittavstemming(grensesnittavstemming2)

        val nyesteAvstemming = avstemmingDao.hentSisteGrensesnittavstemming(Saktype.BARNEPENSJON)
        assertEquals(grensesnittavstemming1, nyesteAvstemming)
    }

    @Test
    fun `skal gi null dersom det ikke finnes noen avstemming`() {
        val nyesteAvstemming = avstemmingDao.hentSisteGrensesnittavstemming(saktype = Saktype.BARNEPENSJON)

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