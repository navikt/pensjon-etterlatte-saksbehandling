package no.nav.etterlatte.utbetaling.iverksetting.utbetaling

import no.nav.etterlatte.utbetaling.TestContainers
import no.nav.etterlatte.utbetaling.attestasjon
import no.nav.etterlatte.utbetaling.config.DataSourceBuilder
import no.nav.etterlatte.utbetaling.iverksetting.oppdrag.OppdragMapper
import no.nav.etterlatte.utbetaling.iverksetting.oppdrag.vedtakId
import no.nav.etterlatte.utbetaling.vedtak
import no.nav.etterlatte.utbetaling.common.Tidspunkt
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
            it.prepareStatement("DELETE FROM utbetalingsoppdrag WHERE vedtak_id IS NOT NULL")
                .apply { execute() }
        }
    }

    @Test
    fun `skal opprette og hente utbetalingsoppdrag`() {
        val vedtak = vedtak()
        val oppdrag = OppdragMapper.oppdragFraVedtak(vedtak, attestasjon(), Tidspunkt.now())
        val opprettet_tidspunkt = Tidspunkt.now()

        utbetalingDao.opprettUtbetaling(vedtak, oppdrag, opprettet_tidspunkt)
        val utbetalingsoppdrag = utbetalingDao.hentUtbetaling(vedtak.vedtakId)

        assertAll(
            "Skal sjekke at utbetalingsoppdrag er korrekt opprettet",
            { assertNotNull(utbetalingsoppdrag?.id) },
            { assertEquals(vedtak.vedtakId, utbetalingsoppdrag?.vedtakId) },
            { assertEquals(vedtak.behandlingsId, utbetalingsoppdrag?.behandlingId) },
            { assertEquals(vedtak.sakId, utbetalingsoppdrag?.sakId) },
            { assertEquals(UtbetalingStatus.SENDT, utbetalingsoppdrag?.status) },
            { assertEquals(vedtak.vedtakId, utbetalingsoppdrag?.vedtakId) },
            {
                assertTrue(
                    utbetalingsoppdrag?.opprettet!!.instant.isAfter(
                        Instant.now().minusSeconds(10)
                    ) and utbetalingsoppdrag.opprettet.instant.isBefore(Instant.now())
                )
            },
            {
                assertTrue(
                    utbetalingsoppdrag!!.endret.instant.isAfter(
                        Instant.now().minusSeconds(10)
                    ) and utbetalingsoppdrag.endret.instant.isBefore(Instant.now())
                )
            },
            {
                assertTrue(
                    utbetalingsoppdrag!!.avstemmingsnoekkel.instant.isAfter(
                        Instant.now().minusSeconds(10)
                    ) and utbetalingsoppdrag.avstemmingsnoekkel.instant.isBefore(Instant.now())
                )
            },
            { assertEquals(vedtak.sakIdGjelderFnr, utbetalingsoppdrag?.foedselsnummer) },
            { assertNotNull(utbetalingsoppdrag?.utgaaendeOppdrag) },
            { assertNull(utbetalingsoppdrag?.kvitteringOppdrag) },
            { assertNull(utbetalingsoppdrag?.kvitteringBeskrivelse) },
            { assertNull(utbetalingsoppdrag?.kvitteringFeilkode) },
            { assertNull(utbetalingsoppdrag?.kvitteringMeldingKode) }
        )
    }

    @Test
    fun `skal sette kvittering paa utbetalingsoppdrag`() {
        val vedtak = vedtak()
        val oppdrag = OppdragMapper.oppdragFraVedtak(vedtak, attestasjon(), Tidspunkt.now())
        val opprettet_tidspunkt = Tidspunkt.now()

        utbetalingDao.opprettUtbetaling(vedtak, oppdrag, opprettet_tidspunkt)

        val oppdragMedKvittering = oppdrag.apply {
            oppdrag110.oppdragsId = 1
            mmel = Mmel().withAlvorlighetsgrad("08").withBeskrMelding("beskrivende melding").withKodeMelding("1234")
        }

        utbetalingDao.oppdaterKvittering(oppdragMedKvittering, Tidspunkt.now())
        val utbetalingsoppdragOppdatert = utbetalingDao.hentUtbetaling(oppdrag.vedtakId())

        assertAll("skal sjekke at kvittering er opprettet korrekt på utbetalingsoppdrag",
            { assertNotNull(utbetalingsoppdragOppdatert?.kvitteringOppdrag) },
            { assertNotNull(utbetalingsoppdragOppdatert?.kvitteringOppdrag?.mmel) },
            {
                assertEquals(
                    oppdragMedKvittering.mmel?.alvorlighetsgrad,
                    utbetalingsoppdragOppdatert?.kvitteringOppdrag?.mmel?.alvorlighetsgrad
                )
            },
            {
                assertEquals(
                    oppdragMedKvittering.oppdrag110.oppdragsId,
                    utbetalingsoppdragOppdatert?.kvitteringOppdrag?.oppdrag110?.oppdragsId
                )
            })
    }

    @Test
    fun `skal oppdatere status paa utbetalingsoppdrag`() {
        val vedtak = vedtak()
        val oppdrag = OppdragMapper.oppdragFraVedtak(vedtak, attestasjon(), Tidspunkt.now())
        val opprettet_tidspunkt = Tidspunkt.now()

        val utbetalingsoppdrag = utbetalingDao.opprettUtbetaling(vedtak, oppdrag, opprettet_tidspunkt)

        assertNotNull(utbetalingsoppdrag)
        assertEquals(UtbetalingStatus.SENDT, utbetalingsoppdrag.status)

        val utbetalingsoppdragOppdatert = utbetalingDao.oppdaterStatus(
            vedtakId = vedtak.vedtakId,
            status = UtbetalingStatus.GODKJENT,
            endret = Tidspunkt.now()
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