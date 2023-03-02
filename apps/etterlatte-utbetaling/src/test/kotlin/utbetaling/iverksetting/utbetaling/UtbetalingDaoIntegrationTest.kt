package no.nav.etterlatte.utbetaling.iverksetting.utbetaling

import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.midnattNorskTid
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.utbetaling.TestContainers
import no.nav.etterlatte.utbetaling.iverksetting.oppdrag.OppdragMapper
import no.nav.etterlatte.utbetaling.iverksetting.oppdrag.vedtakId
import no.nav.etterlatte.utbetaling.oppdrag
import no.nav.etterlatte.utbetaling.utbetaling
import no.nav.etterlatte.utbetaling.utbetalingslinje
import no.trygdeetaten.skjema.oppdrag.Mmel
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.junit.jupiter.Container
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.*
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

        dataSource = DataSourceBuilder.createDataSource(
            jdbcUrl = postgreSQLContainer.jdbcUrl,
            username = postgreSQLContainer.username,
            password = postgreSQLContainer.password
        )

        utbetalingDao = UtbetalingDao(dataSource)
        dataSource.migrate()
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

        assertAll(
            "Skal sjekke at utbetaling er korrekt opprettet",
            { assertNotNull(opprettetUtbetaling?.id) },
            { assertEquals(utbetaling.sakId.value, opprettetUtbetaling?.sakId?.value) },
            { assertEquals(utbetaling.behandlingId.value, opprettetUtbetaling?.behandlingId?.value) },
            { assertEquals(utbetaling.vedtakId.value, opprettetUtbetaling?.vedtakId?.value) },
            {
                assertTrue(
                    opprettetUtbetaling?.opprettet!!.instant.isAfter(
                        Tidspunkt.now().minus(10, ChronoUnit.SECONDS).instant
                    ) and opprettetUtbetaling.opprettet.isBefore(Tidspunkt.now())
                )
            },
            {
                assertTrue(
                    opprettetUtbetaling!!.endret.instant.isAfter(
                        Tidspunkt.now().minus(10, ChronoUnit.SECONDS).instant
                    ) and opprettetUtbetaling.endret.isBefore(Tidspunkt.now())
                )
            },
            {
                assertTrue(
                    opprettetUtbetaling!!.avstemmingsnoekkel.instant.isAfter(
                        Tidspunkt.now().minus(10, ChronoUnit.SECONDS).instant
                    ) and opprettetUtbetaling.avstemmingsnoekkel.isBefore(Tidspunkt.now())
                )
            },
            { assertEquals(utbetaling.stoenadsmottaker.value, opprettetUtbetaling?.stoenadsmottaker?.value) },
            { assertEquals(utbetaling.saksbehandler.value, opprettetUtbetaling?.saksbehandler?.value) },
            { assertEquals(utbetaling.attestant.value, opprettetUtbetaling?.attestant?.value) },
            { assertNotNull(opprettetUtbetaling?.vedtak) },
            { assertNotNull(opprettetUtbetaling?.oppdrag) },
            { assertNull(opprettetUtbetaling?.kvittering) },
            { assertEquals(UtbetalingStatus.MOTTATT, opprettetUtbetaling?.status()) }
        )
    }

    // TODO: nye tester for å sjekke at utbetalingslinjer opprettes korrekt
    @Test
    fun `utbetalingslinjer opprettes korrekt i databasen fra utbetaling`() {
        val utbetalingId = UUID.randomUUID()
        val utbetalingslinjer = listOf(
            utbetalingslinje(utbetalingId, utbetalingslinjeId = 1),
            utbetalingslinje(utbetalingId, utbetalingslinjeId = 2),
            utbetalingslinje(utbetalingId, utbetalingslinjeId = 3)
        )
        val utbetaling = utbetaling(utbetalingId, utbetalingslinjer = utbetalingslinjer)
        val oppdrag = oppdrag(utbetaling)

        val utbetalingFraDatabase = utbetalingDao.opprettUtbetaling(utbetaling.copy(oppdrag = oppdrag))

        assertAll(
            "utbetalingslinjer opprettes korrekt",
            { assertEquals(3, utbetalingFraDatabase.utbetalingslinjer.size) },
            { assertTrue(utbetalingFraDatabase.utbetalingslinjer.any { it.id.value == 1L }) },
            { assertTrue(utbetalingFraDatabase.utbetalingslinjer.any { it.id.value == 2L }) },
            { assertTrue(utbetalingFraDatabase.utbetalingslinjer.any { it.id.value == 3L }) },
            {
                assertTrue(
                    utbetalingFraDatabase.utbetalingslinjer.all { it.type == utbetaling.utbetalingslinjer.first().type }
                )
            },
            {
                assertTrue(
                    utbetalingFraDatabase.utbetalingslinjer.all {
                        it.sakId == utbetaling.utbetalingslinjer.first().sakId
                    }
                )
            },
            {
                assertTrue(
                    utbetalingFraDatabase.utbetalingslinjer.all {
                        it.periode.fra == utbetaling.utbetalingslinjer.first().periode.fra
                    }
                )
            },
            {
                assertTrue(
                    utbetalingFraDatabase.utbetalingslinjer.all {
                        it.periode.til == utbetaling.utbetalingslinjer.first().periode.til
                    }
                )
            },
            {
                assertTrue(
                    utbetalingFraDatabase.utbetalingslinjer.all {
                        it.beloep == utbetaling.utbetalingslinjer.first().beloep
                    }
                )
            }
        )
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

        val utbetalinger = utbetalingDao.hentUtbetalingerForGrensesnittavstemming(jan2, jan5, Saktype.BARNEPENSJON)

        assertAll(
            "3 utbetalinger skal hentes, med vedtak id 2, 3 og 4",
            { assertEquals(3, utbetalinger.size) },
            { assertTrue(utbetalinger.any { it.vedtak.vedtakId == 2L }) },
            { assertTrue(utbetalinger.any { it.vedtak.vedtakId == 3L }) },
            { assertTrue(utbetalinger.any { it.vedtak.vedtakId == 4L }) }
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

        utbetalingDao.oppdaterKvittering(oppdragMedKvittering, Tidspunkt.now(), utbetaling.id)
        val utbetalingOppdatert = utbetalingDao.hentUtbetaling(oppdrag.vedtakId())

        assertAll(
            "skal sjekke at kvittering er opprettet korrekt på utbetaling",
            {
                assertEquals(
                    UtbetalingStatus.AVVIST,
                    utbetalingOppdatert?.status()
                )
            }, // SKAL VEL EGENTLIG VÆRE AVVIST HER??? MÅ GRAVE LITT
            { assertNotNull(utbetalingOppdatert?.kvittering) },
            { assertNotNull(utbetalingOppdatert?.kvittering?.oppdrag?.mmel) },
            {
                assertEquals(
                    oppdragMedKvittering.mmel?.alvorlighetsgrad,
                    utbetalingOppdatert?.kvittering?.oppdrag?.mmel?.alvorlighetsgrad
                )
            }
        )
    }

    @Test
    fun `skal hente liste med dupliserte utbetalingslinjer`() {
        val opprettetTidspunkt = Tidspunkt.now()
        val utbetalingId = UUID.randomUUID()
        val utbetaling = utbetaling(
            id = utbetalingId,
            avstemmingsnoekkel = opprettetTidspunkt,
            utbetalingslinjer = listOf(
                utbetalingslinje(utbetalingId, SakId(1L), 1),
                utbetalingslinje(utbetalingId, SakId(1L), 2),
                utbetalingslinje(utbetalingId, SakId(1L), 3)
            )
        )
        val oppdrag = oppdrag(utbetaling)
        utbetalingDao.opprettUtbetaling(utbetaling.copy(oppdrag = oppdrag))

        val periode = Periode(YearMonth.now(), null)
        val utbetalingslinjeIder = listOf(
            Utbetalingsperiode(1L, periode, BigDecimal(1000), UtbetalingsperiodeType.UTBETALING),
            Utbetalingsperiode(2L, periode, BigDecimal(1000), UtbetalingsperiodeType.UTBETALING),
            Utbetalingsperiode(3L, periode, BigDecimal(1000), UtbetalingsperiodeType.UTBETALING)
        )
        val utbetalingslinjer = utbetalingDao.hentDupliserteUtbetalingslinjer(
            utbetalingslinjeIder,
            utbetalingId = utbetaling.vedtakId.value + 1
        )
        println(utbetalingslinjer)

        assertAll(
            "tre utbetalingslinjer med ider 1,2 og 3 skal hentes fra databasen",
            { assertEquals(3, utbetalingslinjer.size) },
            { assertTrue(utbetalingslinjer.any { it.id.value == 1L }) },
            { assertTrue(utbetalingslinjer.any { it.id.value == 2L }) },
            { assertTrue(utbetalingslinjer.any { it.id.value == 3L }) }
        )
    }

    @Test
    fun `ny utbetalingshendelse lagres paa utbetaling`() {
        val utbetalingId = UUID.randomUUID()
        val utbetaling = utbetaling(
            id = utbetalingId,
            avstemmingsnoekkel = Tidspunkt.now(),
            utbetalingslinjer = listOf(
                utbetalingslinje(utbetalingId, SakId(1L), 1),
                utbetalingslinje(utbetalingId, SakId(1L), 2),
                utbetalingslinje(utbetalingId, SakId(1L), 3)
            )
        )
        val oppdrag = oppdrag(utbetaling)
        utbetalingDao.opprettUtbetaling(utbetaling.copy(oppdrag = oppdrag))

        val utbetalingshendelse = Utbetalingshendelse(utbetalingId = utbetalingId, status = UtbetalingStatus.GODKJENT)
        val oppdatertUtbetaling = utbetalingDao.nyUtbetalingshendelse(utbetaling.vedtakId.value, utbetalingshendelse)

        assertAll(
            "utbetalingshendelse lagres korrekt på utbetaling",
            { assertEquals(utbetalingshendelse.id, oppdatertUtbetaling.utbetalingshendelser.last().id) },
            {
                assertEquals(
                    utbetalingshendelse.utbetalingId,
                    oppdatertUtbetaling.utbetalingshendelser.last().utbetalingId
                )
            },
            { assertEquals(utbetalingshendelse.status, oppdatertUtbetaling.utbetalingshendelser.last().status) },
            { assertEquals(utbetalingshendelse.tidspunkt, oppdatertUtbetaling.utbetalingshendelser.last().tidspunkt) }
        )
    }

    @Test
    fun `skal kun hente godkjente utbetalinger for konsistensavstemming`() {
        val utbetalingId1 = UUID.randomUUID()
        val utbetaling1 =
            utbetaling(
                id = utbetalingId1,
                sakId = SakId(1L),
                sakType = Saktype.BARNEPENSJON,
                vedtakId = 1L,
                utbetalingslinjeId = 1L
            )
        val oppdrag1 = oppdrag(utbetaling1)
        val utbetalingId2 = UUID.randomUUID()
        val utbetaling2 =
            utbetaling(
                id = utbetalingId2,
                sakId = SakId(2L),
                sakType = Saktype.BARNEPENSJON,
                vedtakId = 2L,
                utbetalingslinjeId = 2L
            )
        val oppdrag2 = oppdrag(utbetaling2)
        val utbetalingId3 = UUID.randomUUID()
        val utbetaling3 =
            utbetaling(
                id = utbetalingId3,
                sakId = SakId(3L),
                sakType = Saktype.BARNEPENSJON,
                vedtakId = 3L,
                utbetalingslinjeId = 3L
            )
        val oppdrag3 = oppdrag(utbetaling3)

        utbetalingDao.opprettUtbetaling(utbetaling1.copy(oppdrag = oppdrag1))
        utbetalingDao.opprettUtbetaling(utbetaling2.copy(oppdrag = oppdrag2))
        utbetalingDao.opprettUtbetaling(utbetaling3.copy(oppdrag = oppdrag3))

        utbetalingDao.nyUtbetalingshendelse(
            vedtakId = utbetaling1.vedtakId.value,
            utbetalingshendelse = Utbetalingshendelse(utbetalingId = utbetalingId1, status = UtbetalingStatus.GODKJENT)
        )
        utbetalingDao.nyUtbetalingshendelse(
            vedtakId = utbetaling2.vedtakId.value,
            utbetalingshendelse = Utbetalingshendelse(utbetalingId = utbetalingId2, status = UtbetalingStatus.GODKJENT)
        )

        val utbetalingerFraDao = utbetalingDao.hentUtbetalingerForKonsistensavstemming(
            aktivFraOgMed = LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT).toTidspunkt(),
            opprettetFramTilOgMed = Tidspunkt.now(),
            saktype = Saktype.BARNEPENSJON
        )
        assertTrue(utbetalingerFraDao.any { it.id == utbetalingId1 })
        assertTrue(utbetalingerFraDao.any { it.id == utbetalingId2 })
        assertFalse(utbetalingerFraDao.any { it.id == utbetalingId3 })
    }

    @Test
    fun `skal kun hente aktive utbetalingslinjer for konsistensavstemming som er opprettet foer et gitt tidspunkt`() {
        val utbetalingId1 = UUID.randomUUID()
        val utbetaling1 = // aktiv
            utbetaling(
                id = utbetalingId1,
                sakId = SakId(1L),
                sakType = Saktype.BARNEPENSJON,
                vedtakId = 1L,
                utbetalingslinjeId = 1L,
                periodeFra = LocalDate.now().minusDays(1)
            )
        val oppdrag1 = oppdrag(utbetaling1)
        val utbetalingId2 = UUID.randomUUID()
        val utbetaling2 = // ikke aktiv
            utbetaling(
                id = utbetalingId2,
                sakId = SakId(2L),
                sakType = Saktype.BARNEPENSJON,
                vedtakId = 2L,
                utbetalingslinjeId = 2L,
                periodeFra = LocalDate.now().plusDays(1),
                opprettet = Tidspunkt(Instant.from(LocalDate.now().plusDays(1).midnattNorskTid()))
            )
        val oppdrag2 = oppdrag(utbetaling2)
        val utbetalingId3 = UUID.randomUUID()
        val utbetaling3 = // ikke aktiv
            utbetaling(
                id = utbetalingId3,
                sakId = SakId(3L),
                sakType = Saktype.BARNEPENSJON,
                vedtakId = 3L,
                utbetalingslinjeId = 3L,
                periodeFra = LocalDate.now().minusDays(300),
                periodeTil = LocalDate.now().minusDays(1)
            )
        val oppdrag3 = oppdrag(utbetaling3)

        utbetalingDao.opprettUtbetaling(utbetaling1.copy(oppdrag = oppdrag1))
        utbetalingDao.opprettUtbetaling(utbetaling2.copy(oppdrag = oppdrag2))
        utbetalingDao.opprettUtbetaling(utbetaling3.copy(oppdrag = oppdrag3))

        utbetalingDao.nyUtbetalingshendelse(
            vedtakId = utbetaling1.vedtakId.value,
            utbetalingshendelse = Utbetalingshendelse(utbetalingId = utbetalingId1, status = UtbetalingStatus.GODKJENT)
        )
        utbetalingDao.nyUtbetalingshendelse(
            vedtakId = utbetaling2.vedtakId.value,
            utbetalingshendelse = Utbetalingshendelse(utbetalingId = utbetalingId2, status = UtbetalingStatus.GODKJENT)
        )
        utbetalingDao.nyUtbetalingshendelse(
            vedtakId = utbetaling3.vedtakId.value,
            utbetalingshendelse = Utbetalingshendelse(utbetalingId = utbetalingId3, status = UtbetalingStatus.GODKJENT)
        )

        val utbetalingerFraDao = utbetalingDao.hentUtbetalingerForKonsistensavstemming(
            aktivFraOgMed = Tidspunkt.now(),
            opprettetFramTilOgMed = Tidspunkt.now(),
            saktype = Saktype.BARNEPENSJON
        )
        assertTrue(utbetalingerFraDao.any { it.id == utbetalingId1 }) // aktiv
        assertFalse(utbetalingerFraDao.any { it.id == utbetalingId2 }) // ikke aktiv
        assertFalse(utbetalingerFraDao.any { it.id == utbetalingId3 }) // ikke aktiv
    }

    @AfterEach
    fun afterEach() {
        cleanDatabase()
    }

    @AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()
    }

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
}