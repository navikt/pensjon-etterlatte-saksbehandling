package no.nav.etterlatte.utbetaling.grensesnittavstemming

import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.utbetaling.TestContainers
import no.nav.etterlatte.utbetaling.avstemming.Konsistensavstemming
import no.nav.etterlatte.utbetaling.avstemming.avstemmingsdata.KonsistensavstemmingDataMapper
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Saktype
import no.nav.etterlatte.utbetaling.oppdragForKonsistensavstemming
import no.nav.etterlatte.utbetaling.oppdragslinjeForKonsistensavstemming
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.junit.jupiter.Container
import java.time.LocalDate
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

        dataSource = DataSourceBuilder.createDataSource(
            jdbcUrl = postgreSQLContainer.jdbcUrl,
            username = postgreSQLContainer.username,
            password = postgreSQLContainer.password
        )

        avstemmingDao = AvstemmingDao(dataSource)
        dataSource.migrate()
    }

    @Test
    fun `skal opprette konsistensavstemming for barnepensjon`() {
        val konsistensavstemmingMedData = opprettKonsistensavstemmingMedData()

        val antallRaderOppdatert = avstemmingDao.opprettKonsistensavstemming(konsistensavstemmingMedData)
        assertEquals(1, antallRaderOppdatert)
    }

    @Test
    fun `skal hente siste konsistensavstemming for barnepensjon`() {
        val konsistensavstemming1 = opprettKonsistensavstemmingMedData(Tidspunkt.now())
        val konsistensavstemming2 = opprettKonsistensavstemmingMedData(Tidspunkt.now().minus(3, ChronoUnit.DAYS))
        val konsistensavstemming3 = opprettKonsistensavstemmingMedData(Tidspunkt.now().minus(6, ChronoUnit.DAYS))

        avstemmingDao.opprettKonsistensavstemming(konsistensavstemming1)
        avstemmingDao.opprettKonsistensavstemming(konsistensavstemming2)
        avstemmingDao.opprettKonsistensavstemming(konsistensavstemming3)

        val sisteKonsistensavstemming = avstemmingDao.hentSisteKonsistensavsvemming(Saktype.BARNEPENSJON)
        assertEquals(konsistensavstemming1, sisteKonsistensavstemming)
    }

    @Test
    fun `skal opprette grensesnittavstemming for barnepensjon`() {
        val grensesnittavstemming = Grensesnittavstemming(
            periode = Avstemmingsperiode(
                fraOgMed = Tidspunkt.now().minus(1, ChronoUnit.DAYS),
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
    fun `skal hente nyeste grensesnittavstemming`() {
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
    fun `skal gi null dersom det ikke finnes noen grensesnittavstemming`() {
        val nyesteAvstemming = avstemmingDao.hentSisteGrensesnittavstemming(saktype = Saktype.BARNEPENSJON)

        assertNull(nyesteAvstemming)
    }

    @Test
    fun `skal gi null dersom det ikke finnes noen konsistensavstemming`() {
        val nyesteAvstemming = avstemmingDao.hentSisteKonsistensavsvemming(saktype = Saktype.BARNEPENSJON)

        assertNull(nyesteAvstemming)
    }

    @AfterEach
    fun afterEach() {
        dataSource.connection.use {
            it.prepareStatement(""" TRUNCATE avstemming""").execute()
        }
    }

    @AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()
    }

    fun opprettKonsistensavstemmingMedData(opprettetTilOgMed: Tidspunkt = Tidspunkt.now()): Konsistensavstemming {
        val oppdragslinjer = listOf(oppdragslinjeForKonsistensavstemming(fraOgMed = LocalDate.of(2022, 10, 7)))
        val oppdrag = oppdragForKonsistensavstemming(oppdragslinjeForKonsistensavstemming = oppdragslinjer)
        val konsistensavstemmingUtenData = Konsistensavstemming(
            id = UUIDBase64(),
            sakType = Saktype.BARNEPENSJON,
            opprettet = Tidspunkt.now(),
            avstemmingsdata = null,
            loependeFraOgMed = Tidspunkt.now(),
            opprettetTilOgMed = opprettetTilOgMed,
            loependeUtbetalinger = listOf(oppdrag)

        )
        val avstemmingsdata = KonsistensavstemmingDataMapper(konsistensavstemmingUtenData).opprettAvstemmingsmelding()
        return konsistensavstemmingUtenData.copy(avstemmingsdata = avstemmingsdata.joinToString("\n"))
    }
}