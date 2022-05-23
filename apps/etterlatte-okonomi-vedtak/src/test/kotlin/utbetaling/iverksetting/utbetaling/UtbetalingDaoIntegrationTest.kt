package no.nav.etterlatte.utbetaling.iverksetting.utbetaling

import no.nav.etterlatte.utbetaling.TestContainers
import no.nav.etterlatte.utbetaling.common.Tidspunkt
import no.nav.etterlatte.utbetaling.config.DataSourceBuilder
import no.nav.etterlatte.utbetaling.iverksetting.oppdrag.OppdragMapper
import no.nav.etterlatte.utbetaling.iverksetting.oppdrag.vedtakId
import no.nav.etterlatte.utbetaling.oppdrag
import no.nav.etterlatte.utbetaling.utbetaling
import no.trygdeetaten.skjema.oppdrag.Mmel
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.junit.jupiter.Container
import java.time.Instant
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class UtbetalingDaoIntegrationTest {

    @Container
    private val postgreSQLContainer = TestContainers.postgreSQLContainer

    private lateinit var dataSource: DataSource
    private lateinit var utbetalingDao: UtbetalingDao

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()

        DataSourceBuilder(
            jdbcUrl = postgreSQLContainer.jdbcUrl,
            username = postgreSQLContainer.username,
            password = postgreSQLContainer.password
        ).let {
            dataSource = it.dataSource()
            utbetalingDao = UtbetalingDao(dataSource)
            it.migrate()
        }
    }

    private fun cleanDatabase() {
        dataSource.connection.use {
            it.prepareStatement("TRUNCATE utbetaling CASCADE").apply { execute() }
        }
    }

    @Test
    fun `skal opprette og hente utbetaling`() {

        val utbetaling = utbetaling(avstemmingsnoekkel = Tidspunkt.now())
        val oppdrag = oppdrag(utbetaling)

        utbetalingDao.opprettUtbetaling(utbetaling.copy(oppdrag = oppdrag))
        val opprettetUtbetaling = utbetalingDao.hentUtbetaling(utbetaling.vedtakId.value)

        // TODO: legg til nye felter; iallefall saksbehandler og attestant
        assertAll("Skal sjekke at utbetaling er korrekt opprettet",
            { assertNotNull(opprettetUtbetaling?.id) },
            { assertEquals(utbetaling.vedtakId.value, opprettetUtbetaling?.vedtakId?.value) },
            { assertEquals(utbetaling.behandlingId.value, opprettetUtbetaling?.behandlingId?.value) },
            { assertEquals(utbetaling.sakId.value, opprettetUtbetaling?.sakId?.value) },
            { assertEquals(UtbetalingStatus.SENDT, opprettetUtbetaling?.status) },
            {
                assertTrue(
                    opprettetUtbetaling?.opprettet!!.instant.isAfter(
                        Instant.now().minusSeconds(10)
                    ) and opprettetUtbetaling.opprettet.instant.isBefore(Instant.now())
                )
            },
            {
                assertTrue(
                    opprettetUtbetaling!!.endret.instant.isAfter(
                        Instant.now().minusSeconds(10)
                    ) and opprettetUtbetaling.endret.instant.isBefore(Instant.now())
                )
            },
            {
                assertTrue(
                    opprettetUtbetaling!!.avstemmingsnoekkel.instant.isAfter(
                        Instant.now().minusSeconds(10)
                    ) and opprettetUtbetaling.avstemmingsnoekkel.instant.isBefore(Instant.now())
                )
            },
            { assertEquals(utbetaling.stoenadsmottaker.value, opprettetUtbetaling?.stoenadsmottaker?.value) },
            { assertNotNull(opprettetUtbetaling?.oppdrag) },
            { assertNull(opprettetUtbetaling?.kvittering) }
        )
    }

    // TODO: nye tester for å sjekke at utbetalingslinjer opprettes korrekt

    private fun opprettUtbetaling(
        avstemmingsnoekkel: Tidspunkt = Tidspunkt.now(),
        vedtakId: Long = 1,
        utbetalingslinjeId: Long
    ): Utbetaling {
        val utbetaling = utbetaling(
            avstemmingsnoekkel = avstemmingsnoekkel,
            vedtakId = vedtakId,
            utbetalingslinjeId = utbetalingslinjeId
        )
        val oppdrag = oppdrag(utbetaling)
        return utbetaling.copy(oppdrag = oppdrag)
    }

    @Test
    fun `skal hente alle utbetalinger mellom to tidspunkter`() {
        val jan1 = Tidspunkt(Instant.parse("2022-01-01T00:00:00Z"))
        val jan2 = Tidspunkt(Instant.parse("2022-02-01T00:00:00Z"))
        val jan3 = Tidspunkt(Instant.parse("2022-03-01T00:00:00Z"))
        val jan4 = Tidspunkt(Instant.parse("2022-04-01T00:00:00Z"))
        val jan5 = Tidspunkt(Instant.parse("2022-05-01T00:00:00Z"))
        val jan6 = Tidspunkt(Instant.parse("2022-06-01T00:00:00Z"))

        utbetalingDao.opprettUtbetaling(
            opprettUtbetaling(
                avstemmingsnoekkel = jan1,
                vedtakId = 1,
                utbetalingslinjeId = 1
            )
        )
        utbetalingDao.opprettUtbetaling(
            opprettUtbetaling(
                avstemmingsnoekkel = jan2,
                vedtakId = 2,
                utbetalingslinjeId = 2
            )
        )
        utbetalingDao.opprettUtbetaling(
            opprettUtbetaling(
                avstemmingsnoekkel = jan3,
                vedtakId = 3,
                utbetalingslinjeId = 3
            )
        )
        utbetalingDao.opprettUtbetaling(
            opprettUtbetaling(
                avstemmingsnoekkel = jan4,
                vedtakId = 4,
                utbetalingslinjeId = 4
            )
        )
        utbetalingDao.opprettUtbetaling(
            opprettUtbetaling(
                avstemmingsnoekkel = jan6,
                vedtakId = 6,
                utbetalingslinjeId = 5
            )
        )

        val utbetalinger = utbetalingDao.hentUtbetalinger(jan2, jan5)

        assertAll(
            "3 utbetalinger skal hentes, med vedtak id 2, 3 og 4",
            { assertEquals(3, utbetalinger.size) },
            { assertTrue(utbetalinger.any { it.vedtak.vedtakId == 2L }) },
            { assertTrue(utbetalinger.any { it.vedtak.vedtakId == 3L }) },
            { assertTrue(utbetalinger.any { it.vedtak.vedtakId == 4L }) },
        )
    }

    @Test
    fun `skal sette kvittering paa utbetaling`() {
        val opprettetTidspunkt = Tidspunkt.now()
        val utbetaling = utbetaling(avstemmingsnoekkel = opprettetTidspunkt)
        val oppdrag = OppdragMapper.oppdragFraUtbetaling(utbetaling, true)

        utbetalingDao.opprettUtbetaling(utbetaling.copy(oppdrag = oppdrag))

        val oppdragMedKvittering = oppdrag.apply {
            oppdrag110.oppdragsId = 1
            mmel = Mmel().withAlvorlighetsgrad("08").withBeskrMelding("beskrivende melding").withKodeMelding("1234")
        }

        utbetalingDao.oppdaterKvittering(oppdragMedKvittering, Tidspunkt.now())
        val utbetalingOppdatert = utbetalingDao.hentUtbetaling(oppdrag.vedtakId())

        assertAll("skal sjekke at kvittering er opprettet korrekt på utbetaling",
            { assertNotNull(utbetalingOppdatert?.kvittering) },
            { assertNotNull(utbetalingOppdatert?.kvittering?.oppdrag?.mmel) },
            {
                assertEquals(
                    oppdragMedKvittering.mmel?.alvorlighetsgrad, utbetalingOppdatert?.kvittering?.oppdrag?.mmel?.alvorlighetsgrad
                )
            })
    }

    @Test
    fun `skal oppdatere status paa utbetaling`() {

        val opprettetTidspunkt = Tidspunkt.now()

        val utbetaling = utbetaling(avstemmingsnoekkel = opprettetTidspunkt)
        val oppdrag = oppdrag(utbetaling)
        val opprettetUtbetaling = utbetalingDao.opprettUtbetaling(utbetaling.copy(oppdrag = oppdrag))

        assertNotNull(opprettetUtbetaling)
        assertEquals(UtbetalingStatus.SENDT, opprettetUtbetaling.status)

        val utbetalingOppdatert = utbetalingDao.oppdaterStatus(
            vedtakId = utbetaling.vedtakId.value, status = UtbetalingStatus.GODKJENT, endret = Tidspunkt.now()
        )

        assertEquals(UtbetalingStatus.GODKJENT, utbetalingOppdatert.status)
    }

    @AfterEach
    fun afterEach() {
        cleanDatabase()
    }

    @AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()
    }
}