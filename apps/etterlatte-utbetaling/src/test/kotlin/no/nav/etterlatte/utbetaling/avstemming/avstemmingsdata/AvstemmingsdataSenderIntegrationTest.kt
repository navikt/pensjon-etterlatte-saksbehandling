package no.nav.etterlatte.utbetaling.avstemming.avstemmingsdata

import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.mq.DummyJmsConnectionFactory
import no.nav.etterlatte.mq.EtterlatteJmsConnectionFactory
import no.nav.etterlatte.utbetaling.avstemming.UUIDBase64
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Saktype
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingStatus
import no.nav.etterlatte.utbetaling.mockKonsistensavstemming
import no.nav.etterlatte.utbetaling.oppdragForKonsistensavstemming
import no.nav.etterlatte.utbetaling.oppdragslinjeForKonsistensavstemming
import no.nav.etterlatte.utbetaling.utbetaling
import no.nav.etterlatte.utbetaling.utbetalingshendelse
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AvstemmingsdataSenderIntegrationTest {
    private val jmsConnectionFactory: EtterlatteJmsConnectionFactory = DummyJmsConnectionFactory()
    private lateinit var avstemmingsdataSender: AvstemmingsdataSender

    @BeforeAll
    fun beforeAll() {
        avstemmingsdataSender =
            AvstemmingsdataSender(
                jmsConnectionFactory = jmsConnectionFactory,
                queue = "DEV.QUEUE.1",
            )
    }

    @Test
    fun `skal sende konsistensavstemmingsmeldinger paa koeen`() {
        val oppdragslinjer = listOf(oppdragslinjeForKonsistensavstemming(fraOgMed = LocalDate.of(2022, 10, 7)))
        val oppdrag = oppdragForKonsistensavstemming(oppdragslinjeForKonsistensavstemming = oppdragslinjer)
        val konsistensavstemming =
            mockKonsistensavstemming(
                loependeUtbetalinger = listOf(oppdrag),
                sakType = Saktype.BARNEPENSJON,
            )
        val avstemmingsdata =
            KonsistensavstemmingDataMapper(
                konsistensavstemming,
            ).opprettAvstemmingsmelding(Saktype.BARNEPENSJON)
        avstemmingsdata.forEach {
            val xml = avstemmingsdataSender.sendKonsistensavstemming(it)
            assertNotNull(xml)
        }
    }

    @Test
    fun `skal sende grensesnittavstemmingsmeldinger paa koeen`() {
        val fraOgMed = Tidspunkt.now().minus(1, ChronoUnit.DAYS)
        val til = Tidspunkt.now()

        val utbetalinger =
            listOf(
                utbetaling(utbetalingshendelser = listOf(utbetalingshendelse(status = UtbetalingStatus.FEILET))),
                utbetaling(utbetalingshendelser = listOf(utbetalingshendelse(status = UtbetalingStatus.FEILET))),
                utbetaling(utbetalingshendelser = listOf(utbetalingshendelse(status = UtbetalingStatus.FEILET))),
            )
        val grensesnittavstemmingDataMapper =
            GrensesnittavstemmingDataMapper(utbetalinger, fraOgMed, til, UUIDBase64(), 2)
        val avstemmingsdata = grensesnittavstemmingDataMapper.opprettAvstemmingsmelding(Saktype.BARNEPENSJON)

        avstemmingsdata.forEach {
            val xml = avstemmingsdataSender.sendGrensesnittavstemming(it)
            assertNotNull(xml)
        }
    }

    @AfterAll
    fun afterAll() {
        jmsConnectionFactory.stop()
    }
}
