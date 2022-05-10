package no.nav.etterlatte.utbetaling.grensesnittavstemming

import no.nav.etterlatte.utbetaling.common.Tidspunkt
import no.nav.etterlatte.utbetaling.grensesnittavstemming.avstemmingsdata.AvstemmingsdataMapper
import no.nav.etterlatte.utbetaling.grensesnittavstemming.avstemmingsdata.AvstemmingsdataSender
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingDao
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit

class GrensesnittsavstemmingService(
    private val avstemmingsdataSender: AvstemmingsdataSender,
    private val grensesnittavstemmingDao: GrensesnittavstemmingDao,
    private val utbetalingDao: UtbetalingDao,
    private val clock: Clock
) {

    fun startGrensesnittsavstemming(
        periodeFraOgMed: Tidspunkt = hentFraTid(),
        periodeTil: Tidspunkt = hentTilTid()
    ) {
        logger.info("Avstemmer fra $periodeFraOgMed til $periodeTil")
        val utbetalingsoppdrag = utbetalingDao.hentAlleUtbetalingerMellom(periodeFraOgMed, periodeTil)

        val grensesnittavstemming = Grensesnittavstemming(
            periodeFraOgMed = periodeFraOgMed,
            periodeTil = periodeTil,
            antallOppdrag = utbetalingsoppdrag.size,
            opprettet = Tidspunkt.now(clock)
        )

        val avstemmingMapper = AvstemmingsdataMapper(
            utbetalinger = utbetalingsoppdrag,
            periodeFraOgMed = periodeFraOgMed,
            periodeTil = periodeTil,
            avstemmingId = grensesnittavstemming.id
        )
        val avstemmingsmelding = avstemmingMapper.opprettAvstemmingsmelding()

        avstemmingsmelding.forEachIndexed { index, avstemmingsdata ->
            avstemmingsdataSender.sendAvstemming(avstemmingsdata)
            logger.info("Avstemmingsmelding ${index + 1} av ${avstemmingsmelding.size} overført til Oppdrag")
        }

        grensesnittavstemmingDao.opprettAvstemming(grensesnittavstemming)

        logger.info(
            "Avstemming fra $periodeFraOgMed til $periodeTil fullført - ${utbetalingsoppdrag.size} oppdrag ble avstemt"
        )
    }

    // Setter til-tid kl 00.00 norsk tid
    private fun hentTilTid(): Tidspunkt =
        Tidspunkt.now(clock)
            .toZonedNorskTid()
            .truncatedTo(ChronoUnit.DAYS)
            .toInstant().let {
                Tidspunkt(it)
            }


    private fun hentFraTid() = grensesnittavstemmingDao.hentSisteAvstemming()?.periodeTil ?: MIN_INSTANT

    companion object {
        private val MIN_INSTANT = Tidspunkt(Instant.EPOCH)
        private val logger = LoggerFactory.getLogger(AvstemmingsdataSender::class.java)
    }

}