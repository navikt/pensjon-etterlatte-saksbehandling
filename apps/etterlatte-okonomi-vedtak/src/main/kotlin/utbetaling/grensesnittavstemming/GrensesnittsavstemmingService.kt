package no.nav.etterlatte.utbetaling.grensesnittavstemming

import no.nav.etterlatte.utbetaling.grensesnittavstemming.avstemmingsdata.AvstemmingsdataMapper
import no.nav.etterlatte.utbetaling.grensesnittavstemming.avstemmingsdata.AvstemmingsdataSender
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingDao
import no.nav.su.se.bakover.common.Tidspunkt
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit

class GrensesnittsavstemmingService(
    private val avstemmingsdataSender: AvstemmingsdataSender,
    private val grensesnittavstemmingDao: GrensesnittavstemmingDao,
    private val utbetalingDao: UtbetalingDao, // TODO: vurdere om service bor sendes inn i stedet for dao
    private val clock: Clock
) {

    fun startGrensesnittsavstemming(
        fraOgMed: Tidspunkt = hentFraTid(),
        til: Tidspunkt = hentTilTid()
    ) {
        logger.info("Avstemmer fra $fraOgMed til $til")
        val utbetalingsoppdrag = utbetalingDao.hentAlleUtbetalingerMellom(fraOgMed, til)
        val grensesnittavstemming =
            Grensesnittavstemming(
                fraOgMed = fraOgMed,
                til = til,
                antallAvstemteOppdrag = utbetalingsoppdrag.size,
                opprettet = Tidspunkt.now(clock)
            )
        val avstemmingMapper = AvstemmingsdataMapper(utbetalingsoppdrag, fraOgMed, til, grensesnittavstemming.id)
        val avstemmingsmelding = avstemmingMapper.opprettAvstemmingsmelding()

        avstemmingsmelding.forEachIndexed { index, avstemmingsdata ->
            avstemmingsdataSender.sendAvstemming(avstemmingsdata)
            logger.info("Avstemmingsmelding ${index + 1} av ${avstemmingsmelding.size} overført til Oppdrag")
        }

        grensesnittavstemmingDao.opprettAvstemming(grensesnittavstemming)
        logger.info("Avstemming fra $fraOgMed til $til fullført - ${utbetalingsoppdrag.size} oppdrag ble avstemt")
    }

    // Setter til-tid kl 00.00 norsk tid
    private fun hentTilTid(): Tidspunkt =
        Tidspunkt.now(clock)
            .toZonedNorskTid()
            .truncatedTo(ChronoUnit.DAYS)
            .toInstant().let {
                Tidspunkt(it)
            }


    fun hentFraTid() = grensesnittavstemmingDao.hentSisteAvstemming()?.til ?: MIN_INSTANT

    companion object {
        private val MIN_INSTANT = Tidspunkt(Instant.EPOCH)
        private val logger = LoggerFactory.getLogger(AvstemmingsdataSender::class.java)
    }

}