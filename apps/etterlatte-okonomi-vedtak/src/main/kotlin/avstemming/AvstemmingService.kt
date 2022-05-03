package no.nav.etterlatte.avstemming

import no.nav.etterlatte.oppdrag.UtbetalingsoppdragDao
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class AvstemmingService(
    private val avstemmingSender: AvstemmingSender,
    private val avstemmingDao: AvstemmingDao,
    private val utbetalingsoppdragDao: UtbetalingsoppdragDao,
) {

    fun startAvstemming(
        fraOgMed: LocalDateTime = hentFraTid(),
        til: LocalDateTime = hentTilTid()
    ) {
        logger.info("Avstemmer fra $fraOgMed til $til")
        val utbetalingsoppdrag = utbetalingsoppdragDao.hentAlleUtbetalingsoppdragMellom(fraOgMed, til)
        val avstemming = Avstemming(fraOgMed = fraOgMed, til = til, antallAvstemteOppdrag = utbetalingsoppdrag.size)
        val avstemmingMapper = AvstemmingsdataMapper(utbetalingsoppdrag, fraOgMed, til, avstemming.id)
        val avstemmingsmelding = avstemmingMapper.opprettAvstemmingsmelding()

        avstemmingsmelding.forEachIndexed { index, avstemmingsdata ->
            avstemmingSender.sendAvstemming(avstemmingsdata)
            logger.info("Avstemmingsmelding ${index+1} av ${avstemmingsmelding.size} overført til Oppdrag")
        }

        // TODO dersom vi har null oppdrag - hva skal da lagres?
        avstemmingDao.opprettAvstemming(avstemming)
        logger.info("Avstemming fra $fraOgMed til $til fullført - ${utbetalingsoppdrag.size} oppdrag ble avstemt")
    }

    private fun hentTilTid() = LocalDateTime.now()

    private fun hentFraTid() = avstemmingDao.hentSisteAvstemming()?.til ?: MIN_LOCALDATETIME

    companion object {
        private val MIN_LOCALDATETIME = LocalDateTime.parse("1900-01-01T00:00:00")
        private val logger = LoggerFactory.getLogger(AvstemmingSender::class.java)
    }

}