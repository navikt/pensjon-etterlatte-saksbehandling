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
        avstemmingNokkelFra: LocalDateTime = avstemmingDao.hentSisteAvstemming()?.avstemmingsnokkelTilOgMed ?: LocalDateTime.MIN,
        avstemmingNokkelTil: LocalDateTime = LocalDateTime.now()
    ) {
        logger.info("Avstemmer data fra $avstemmingNokkelFra til $avstemmingNokkelTil")
        val utbetalingsoppdrag = utbetalingsoppdragDao.hentAlleUtbetalingsoppdragMellom(
            fraOgMed = avstemmingNokkelFra,
            tilOgMed = avstemmingNokkelTil
        )

        val avstemming = Avstemming(
            avstemmingsnokkelTilOgMed = avstemmingNokkelTil,
            antallAvstemteOppdrag = utbetalingsoppdrag.size
        )

        val avstemmingsmelding = AvstemmingsdataMapper(utbetalingsoppdrag, avstemming.id).opprettAvstemmingsmelding()

        avstemmingSender.sendAvstemming(avstemmingsmelding)
        avstemmingDao.opprettAvstemming(avstemming)
        logger.info("Avstemming fullført")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AvstemmingSender::class.java)
    }

}