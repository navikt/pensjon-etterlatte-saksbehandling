package no.nav.etterlatte.oppdrag

import no.nav.etterlatte.attestasjon
import no.nav.etterlatte.config.DataSourceBuilder
import no.nav.etterlatte.domain.UtbetalingsoppdragStatus
import no.nav.etterlatte.util.TestContainers
import no.nav.etterlatte.vedtak
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
import java.time.LocalDateTime
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class UtbetalingsoppdragDaoIntegrationTest {

    @Container
    private val postgreSQLContainer = TestContainers.postgreSQLContainer

    private lateinit var dataSource: DataSource
    private lateinit var utbetalingsoppdragDao: UtbetalingsoppdragDao

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()

        DataSourceBuilder(
            jdbcUrl = postgreSQLContainer.jdbcUrl,
            username = postgreSQLContainer.username,
            password = postgreSQLContainer.password
        ).let {
            dataSource = it.dataSource()
            utbetalingsoppdragDao = UtbetalingsoppdragDao(dataSource)
            it.migrate()
        }
    }

    @AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()

    }

    @AfterEach
    fun afterEach() {
        cleanDatabase()
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
        val oppdrag = OppdragMapper.oppdragFraVedtak(vedtak, attestasjon(), LocalDateTime.now())
        val opprettet_tidspunkt = LocalDateTime.now()

        utbetalingsoppdragDao.opprettUtbetalingsoppdrag(vedtak, oppdrag, opprettet_tidspunkt)
        val utbetalingsoppdrag = utbetalingsoppdragDao.hentUtbetalingsoppdrag(vedtak.vedtakId)

        assertAll(
            "Skal sjekke at utbetalingsoppdrag er korrekt opprettet",
            { assertNotNull(utbetalingsoppdrag?.id) },
            { assertEquals(vedtak.vedtakId, utbetalingsoppdrag?.vedtakId) },
            { assertEquals(vedtak.behandlingsId, utbetalingsoppdrag?.behandlingId) },
            { assertEquals(vedtak.sakId, utbetalingsoppdrag?.sakId) },
            { assertEquals(UtbetalingsoppdragStatus.SENDT, utbetalingsoppdrag?.status) },
            { assertEquals(vedtak.vedtakId, utbetalingsoppdrag?.vedtakId) },
            {
                assertTrue(
                    utbetalingsoppdrag?.opprettet!!.isAfter(
                        LocalDateTime.now().minusSeconds(10)
                    ) and utbetalingsoppdrag.opprettet.isBefore(LocalDateTime.now())
                )
            },
            {
                assertTrue(
                    utbetalingsoppdrag!!.endret.isAfter(
                        LocalDateTime.now().minusSeconds(10)
                    ) and utbetalingsoppdrag.endret.isBefore(LocalDateTime.now())
                )
            },
            {
                assertTrue(
                    utbetalingsoppdrag!!.avstemmingsnoekkel.isAfter(
                        LocalDateTime.now().minusSeconds(10)
                    ) and utbetalingsoppdrag.avstemmingsnoekkel.isBefore(LocalDateTime.now())
                )
            },
            { assertEquals(vedtak.sakIdGjelderFnr, utbetalingsoppdrag?.foedselsnummer) },
            { assertNotNull(utbetalingsoppdrag?.utgaaendeOppdrag) },
            { assertNull(utbetalingsoppdrag?.oppdragKvittering) },
            { assertNull(utbetalingsoppdrag?.beskrivelseOppdrag) },
            { assertNull(utbetalingsoppdrag?.feilkodeOppdrag) },
            { assertNull(utbetalingsoppdrag?.meldingKodeOppdrag) }
        )
    }

    @Test
    fun `skal sette kvittering paa utbetalingsoppdrag`() {
        val vedtak = vedtak()
        val oppdrag = OppdragMapper.oppdragFraVedtak(vedtak, attestasjon(), LocalDateTime.now())
        val opprettet_tidspunkt = LocalDateTime.now()

        utbetalingsoppdragDao.opprettUtbetalingsoppdrag(vedtak, oppdrag, opprettet_tidspunkt)

        val oppdragMedKvittering = oppdrag.apply {
            oppdrag110.oppdragsId = 1
            mmel = Mmel().withAlvorlighetsgrad("08").withBeskrMelding("beskrivende melding").withKodeMelding("1234")
        }

        utbetalingsoppdragDao.oppdaterKvittering(oppdragMedKvittering)
        val utbetalingsoppdragOppdatert = utbetalingsoppdragDao.hentUtbetalingsoppdrag(oppdrag.vedtakId())

        assertAll("skal sjekke at kvittering er opprettet korrekt på utbetalingsoppdrag",
            { assertNotNull(utbetalingsoppdragOppdatert?.oppdragKvittering) },
            { assertNotNull(utbetalingsoppdragOppdatert?.oppdragKvittering?.mmel) },
            {
                assertEquals(
                    oppdragMedKvittering.mmel?.alvorlighetsgrad,
                    utbetalingsoppdragOppdatert?.oppdragKvittering?.mmel?.alvorlighetsgrad
                )
            },
            {
                assertEquals(
                    oppdragMedKvittering.oppdrag110.oppdragsId,
                    utbetalingsoppdragOppdatert?.oppdragKvittering?.oppdrag110?.oppdragsId
                )
            })
    }

    @Test
    fun `skal oppdatere status paa utbetalingsoppdrag`() {
        val vedtak = vedtak()
        val oppdrag = OppdragMapper.oppdragFraVedtak(vedtak, attestasjon(), LocalDateTime.now())
        val opprettet_tidspunkt = LocalDateTime.now()

        val utbetalingsoppdrag = utbetalingsoppdragDao.opprettUtbetalingsoppdrag(vedtak, oppdrag, opprettet_tidspunkt)

        assertNotNull(utbetalingsoppdrag)
        assertEquals(UtbetalingsoppdragStatus.SENDT, utbetalingsoppdrag.status)

        val utbetalingsoppdragOppdatert = utbetalingsoppdragDao.oppdaterStatus(
            vedtakId = vedtak.vedtakId,
            status = UtbetalingsoppdragStatus.GODKJENT
        )

        assertEquals(UtbetalingsoppdragStatus.GODKJENT, utbetalingsoppdragOppdatert.status)
    }
}