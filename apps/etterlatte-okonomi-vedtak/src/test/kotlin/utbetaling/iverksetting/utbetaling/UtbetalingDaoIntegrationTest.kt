package no.nav.etterlatte.utbetaling.iverksetting.utbetaling

import no.nav.etterlatte.utbetaling.TestContainers
import no.nav.etterlatte.utbetaling.attestasjon
import no.nav.etterlatte.utbetaling.common.Tidspunkt
import no.nav.etterlatte.utbetaling.config.DataSourceBuilder
import no.nav.etterlatte.utbetaling.iverksetting.oppdrag.OppdragMapper
import no.nav.etterlatte.utbetaling.iverksetting.oppdrag.vedtakId
import no.nav.etterlatte.utbetaling.vedtak
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
            it.prepareStatement("TRUNCATE utbetalingsoppdrag").apply { execute() }
        }
    }

    @Test
    fun `skal opprette og hente utbetalingsoppdrag`() {
        val vedtak = vedtak()
        val oppdrag = OppdragMapper.oppdragFraVedtak(vedtak, attestasjon(), Tidspunkt.now())
        val opprettetTidspunkt = Tidspunkt.now()

        utbetalingDao.opprettUtbetaling(vedtak, oppdrag, opprettetTidspunkt)
        val utbetaling = utbetalingDao.hentUtbetaling(vedtak.vedtakId)

        assertAll("Skal sjekke at utbetalingsoppdrag er korrekt opprettet",
            { assertNotNull(utbetaling?.id) },
            { assertEquals(vedtak.vedtakId, utbetaling?.vedtakId?.value) },
            { assertEquals(vedtak.behandlingsId, utbetaling?.behandlingId?.value) },
            { assertEquals(vedtak.sakId, utbetaling?.sakId?.value) },
            { assertEquals(UtbetalingStatus.SENDT, utbetaling?.status) },
            {
                assertTrue(
                    utbetaling?.opprettet!!.instant.isAfter(
                        Instant.now().minusSeconds(10)
                    ) and utbetaling.opprettet.instant.isBefore(Instant.now())
                )
            },
            {
                assertTrue(
                    utbetaling!!.endret.instant.isAfter(
                        Instant.now().minusSeconds(10)
                    ) and utbetaling.endret.instant.isBefore(Instant.now())
                )
            },
            {
                assertTrue(
                    utbetaling!!.avstemmingsnoekkel.instant.isAfter(
                        Instant.now().minusSeconds(10)
                    ) and utbetaling.avstemmingsnoekkel.instant.isBefore(Instant.now())
                )
            },
            { assertEquals(vedtak.sakIdGjelderFnr, utbetaling?.foedselsnummer?.value) },
            { assertNotNull(utbetaling?.oppdrag) },
            { assertNull(utbetaling?.kvittering) },
            { assertNull(utbetaling?.kvitteringBeskrivelse) },
            { assertNull(utbetaling?.kvitteringFeilkode) },
            { assertNull(utbetaling?.kvitteringMeldingKode) })
    }

    @Test
    fun `skal hente alle utbetalinger mellom to tidspunkter`() {
        val attestasjon = attestasjon()

        val vedtak1 = vedtak("1")
        val vedtak2 = vedtak("2")
        val vedtak3 = vedtak("3")
        val vedtak4 = vedtak("4")
        val vedtak5 = vedtak("5")

        val jan1 = Tidspunkt(Instant.parse("2022-01-01T00:00:00Z"))
        val jan2 = Tidspunkt(Instant.parse("2022-02-01T00:00:00Z"))
        val jan3 = Tidspunkt(Instant.parse("2022-03-01T00:00:00Z"))
        val jan4 = Tidspunkt(Instant.parse("2022-04-01T00:00:00Z"))
        val jan5 = Tidspunkt(Instant.parse("2022-05-01T00:00:00Z"))
        val jan6 = Tidspunkt(Instant.parse("2022-06-01T00:00:00Z"))

        val oppdrag1 = OppdragMapper.oppdragFraVedtak(vedtak1, attestasjon, jan1)
        val oppdrag2 = OppdragMapper.oppdragFraVedtak(vedtak2, attestasjon, jan2)
        val oppdrag3 = OppdragMapper.oppdragFraVedtak(vedtak3, attestasjon, jan3)
        val oppdrag4 = OppdragMapper.oppdragFraVedtak(vedtak4, attestasjon, jan4)
        val oppdrag5 = OppdragMapper.oppdragFraVedtak(vedtak5, attestasjon, jan6)

        utbetalingDao.opprettUtbetaling(vedtak1, oppdrag1, jan1)
        utbetalingDao.opprettUtbetaling(vedtak2, oppdrag2, jan2)
        utbetalingDao.opprettUtbetaling(vedtak3, oppdrag3, jan3)
        utbetalingDao.opprettUtbetaling(vedtak4, oppdrag4, jan4)
        utbetalingDao.opprettUtbetaling(vedtak5, oppdrag5, jan6)

        val utbetalinger = utbetalingDao.hentAlleUtbetalingerMellom(jan2, jan5)

        assertAll(
            "3 utbetalinger skal hentes, med vedtak id 2, 3 og 4",
            { assertEquals(3, utbetalinger.size) },
            { assertTrue(utbetalinger.any { it.vedtak.vedtakId == "2" }) },
            { assertTrue(utbetalinger.any { it.vedtak.vedtakId == "3" }) },
            { assertTrue(utbetalinger.any { it.vedtak.vedtakId == "4" }) },
        )
    }


    @Test
    fun `skal sette kvittering paa utbetalingsoppdrag`() {
        val vedtak = vedtak()
        val oppdrag = OppdragMapper.oppdragFraVedtak(vedtak, attestasjon(), Tidspunkt.now())
        val opprettetTidspunkt = Tidspunkt.now()

        utbetalingDao.opprettUtbetaling(vedtak, oppdrag, opprettetTidspunkt)

        val oppdragMedKvittering = oppdrag.apply {
            oppdrag110.oppdragsId = 1
            mmel = Mmel().withAlvorlighetsgrad("08").withBeskrMelding("beskrivende melding").withKodeMelding("1234")
        }

        utbetalingDao.oppdaterKvittering(oppdragMedKvittering, Tidspunkt.now())
        val utbetalingOppdatert = utbetalingDao.hentUtbetaling(oppdrag.vedtakId())

        assertAll("skal sjekke at kvittering er opprettet korrekt på utbetalingsoppdrag",
            { assertNotNull(utbetalingOppdatert?.kvittering) },
            { assertNotNull(utbetalingOppdatert?.kvittering?.mmel) },
            {
                assertEquals(
                    oppdragMedKvittering.mmel?.alvorlighetsgrad, utbetalingOppdatert?.kvittering?.mmel?.alvorlighetsgrad
                )
            },
            {
                assertEquals(
                    oppdragMedKvittering.oppdrag110.oppdragsId, utbetalingOppdatert?.kvittering?.oppdrag110?.oppdragsId
                )
            })
    }

    @Test
    fun `skal oppdatere status paa utbetalingsoppdrag`() {
        val vedtak = vedtak()
        val oppdrag = OppdragMapper.oppdragFraVedtak(vedtak, attestasjon(), Tidspunkt.now())
        val opprettetTidspunkt = Tidspunkt.now()

        val utbetaling = utbetalingDao.opprettUtbetaling(vedtak, oppdrag, opprettetTidspunkt)

        assertNotNull(utbetaling)
        assertEquals(UtbetalingStatus.SENDT, utbetaling.status)

        val utbetalingsoppdragOppdatert = utbetalingDao.oppdaterStatus(
            vedtakId = vedtak.vedtakId, status = UtbetalingStatus.GODKJENT, endret = Tidspunkt.now()
        )

        assertEquals(UtbetalingStatus.GODKJENT, utbetalingsoppdragOppdatert.status)
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