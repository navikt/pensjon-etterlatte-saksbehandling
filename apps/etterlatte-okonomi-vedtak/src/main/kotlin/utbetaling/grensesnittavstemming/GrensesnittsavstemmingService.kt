package no.nav.etterlatte.utbetaling.grensesnittavstemming

import no.nav.etterlatte.utbetaling.common.Tidspunkt
import no.nav.etterlatte.utbetaling.common.tidspunktMidnattIdag
import no.nav.etterlatte.utbetaling.grensesnittavstemming.avstemmingsdata.AvstemmingsdataMapper
import no.nav.etterlatte.utbetaling.grensesnittavstemming.avstemmingsdata.AvstemmingsdataSender
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingDao
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.Instant

class GrensesnittsavstemmingService(
    private val avstemmingsdataSender: AvstemmingsdataSender,
    private val grensesnittavstemmingDao: GrensesnittavstemmingDao,
    private val utbetalingDao: UtbetalingDao,
    private val clock: Clock
) {

    fun hentNestePeriode() = Avstemmingsperiode(
        fraOgMed = grensesnittavstemmingDao.hentSisteAvstemming()?.periode?.til ?: MIN_INSTANT,
        til = tidspunktMidnattIdag(clock)
    )

    fun startGrensesnittsavstemming(
        periode: Avstemmingsperiode = hentNestePeriode()
    ) {
        logger.info("Avstemmer fra ${periode.fraOgMed} til ${periode.til}")
        val utbetalinger = utbetalingDao.hentAlleUtbetalingerMellom(periode.fraOgMed, periode.til)
        val avstemmingId = UUIDBase64()

        val avstemmingsdataMapper = AvstemmingsdataMapper(utbetalinger, periode.fraOgMed, periode.til, avstemmingId)
        val avstemmingsdataListe = avstemmingsdataMapper.opprettAvstemmingsmelding()

        val sendtAvstemmingsdata = avstemmingsdataListe.mapIndexed { index, avstemmingsdata ->
            val sendtAvstemmingsdata = avstemmingsdataSender.sendAvstemming(avstemmingsdata)
            logger.info("Avstemmingsmelding ${index + 1} av ${avstemmingsdataListe.size} overført til Oppdrag")
            sendtAvstemmingsdata
        }

        grensesnittavstemmingDao.opprettAvstemming(
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