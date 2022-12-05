package no.nav.etterlatte.utbetaling.grensesnittavstemming

import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.utbetaling.common.tidspunktMidnattIdag
import no.nav.etterlatte.utbetaling.grensesnittavstemming.avstemmingsdata.AvstemmingsdataSender
import no.nav.etterlatte.utbetaling.grensesnittavstemming.avstemmingsdata.GrensesnittavstemmingDataMapper
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingDao
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.Instant

class GrensesnittsavstemmingService(
    private val avstemmingsdataSender: AvstemmingsdataSender,
    private val avstemmingDao: AvstemmingDao,
    private val utbetalingDao: UtbetalingDao,
    private val clock: Clock
) {

    fun hentNestePeriode() = Avstemmingsperiode(
        fraOgMed = avstemmingDao.hentSisteGrensesnittavstemming()?.periode?.til ?: MIN_INSTANT,
        til = tidspunktMidnattIdag(clock)
    )

    fun startGrensesnittsavstemming(
        periode: Avstemmingsperiode = hentNestePeriode()
    ) {
        logger.info("Avstemmer fra ${periode.fraOgMed} til ${periode.til}")
        val utbetalinger = utbetalingDao.hentUtbetalinger(periode.fraOgMed, periode.til)
        val avstemmingId = UUIDBase64()

        val grensesnittavstemmingDataMapper =
            GrensesnittavstemmingDataMapper(utbetalinger, periode.fraOgMed, periode.til, avstemmingId)
        val avstemmingsdataListe = grensesnittavstemmingDataMapper.opprettAvstemmingsmelding()

        val sendtAvstemmingsdata = avstemmingsdataListe.mapIndexed { index, avstemmingsdata ->
            val sendtAvstemmingsdata = avstemmingsdataSender.sendGrensesnittavstemming(avstemmingsdata)
            logger.info("Avstemmingsmelding ${index + 1} av ${avstemmingsdataListe.size} overført til Oppdrag")
            sendtAvstemmingsdata
        }

        avstemmingDao.opprettGrensesnittavstemming(
            Grensesnittavstemming(
                id = avstemmingId,
                periode = periode,
                antallOppdrag = utbetalinger.size,
                opprettet = Tidspunkt.now(clock),
                avstemmingsdata = sendtAvstemmingsdata.joinToString("\n")
            )
        )

        logger.info(
            "Avstemming fra ${periode.fraOgMed} til ${periode.til} fullført - ${utbetalinger.size} oppdrag ble avstemt"
        )
    }

    companion object {
        private val MIN_INSTANT = Tidspunkt(Instant.EPOCH)
        private val logger = LoggerFactory.getLogger(AvstemmingsdataSender::class.java)
    }
}