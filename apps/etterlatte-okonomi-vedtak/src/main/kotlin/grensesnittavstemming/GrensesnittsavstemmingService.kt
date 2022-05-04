package no.nav.etterlatte.grensesnittavstemming

import no.nav.etterlatte.utbetaling.UtbetalingDao
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class GrensesnittsavstemmingService(
    private val avstemmingsdataSender: AvstemmingsdataSender,
    private val grensesnittavstemmingDao: GrensesnittavstemmingDao,
    private val utbetalingDao: UtbetalingDao, // TODO: vurdere om service bor sendes inn i stedet for dao
) {

    fun startGrensesnittsavstemming(
        fraOgMed: LocalDateTime = hentFraTid(),
        til: LocalDateTime = hentTilTid()
    ) {
        logger.info("Avstemmer fra $fraOgMed til $til")
        val utbetalingsoppdrag = utbetalingDao.hentAlleUtbetalingerMellom(fraOgMed, til)
        val grensesnittavstemming =
            Grensesnittavstemming(fraOgMed = fraOgMed, til = til, antallAvstemteOppdrag = utbetalingsoppdrag.size)
        val avstemmingMapper = AvstemmingsdataMapper(utbetalingsoppdrag, fraOgMed, til, grensesnittavstemming.id)
        val avstemmingsmelding = avstemmingMapper.opprettAvstemmingsmelding()

        avstemmingsmelding.forEachIndexed { index, avstemmingsdata ->
            avstemmingsdataSender.sendAvstemming(avstemmingsdata)
            logger.info("Avstemmingsmelding ${index + 1} av ${avstemmingsmelding.size} overført til Oppdrag")
        }

        // TODO dersom vi har null oppdrag - hva skal da lagres?
        grensesnittavstemmingDao.opprettAvstemming(grensesnittavstemming)
        logger.info("Avstemming fra $fraOgMed til $til fullført - ${utbetalingsoppdrag.size} oppdrag ble avstemt")
    }

    private fun hentTilTid() = LocalDateTime.now()

    private fun hentFraTid() = grensesnittavstemmingDao.hentSisteAvstemming()?.til ?: MIN_LOCALDATETIME

    companion object {
        private val MIN_LOCALDATETIME = LocalDateTime.parse("1900-01-01T00:00:00")
        private val logger = LoggerFactory.getLogger(AvstemmingsdataSender::class.java)
    }

}