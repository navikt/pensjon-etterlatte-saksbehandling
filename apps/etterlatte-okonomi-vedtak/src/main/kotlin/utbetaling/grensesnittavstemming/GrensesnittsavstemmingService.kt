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
        periodeFraOgMed: Tidspunkt = hentFraTidForrigeAvstemming(),
        periodeTil: Tidspunkt = hentTilTidStartAvDagen()
    ) {
        logger.info("Avstemmer fra $periodeFraOgMed til $periodeTil")
        if (periodeFraOgMed != periodeTil) {
            val utbetalinger = utbetalingDao.hentAlleUtbetalingerMellom(periodeFraOgMed, periodeTil)
            val avstemming = Grensesnittavstemming(
                periodeFraOgMed = periodeFraOgMed,
                periodeTil = periodeTil,
                antallOppdrag = utbetalinger.size,
                opprettet = Tidspunkt.now(clock)
            )

            val avstemmingsdataMapper = AvstemmingsdataMapper(utbetalinger, periodeFraOgMed, periodeTil, avstemming.id)
            val avstemmingsdataListe = avstemmingsdataMapper.opprettAvstemmingsmelding()

            val sendtAvstemmingsdata = avstemmingsdataListe.mapIndexed { index, avstemmingsdata ->
                val sendtAvstemmingsdata = avstemmingsdataSender.sendAvstemming(avstemmingsdata)
                logger.info("Avstemmingsmelding ${index + 1} av ${avstemmingsdataListe.size} overført til Oppdrag")
                sendtAvstemmingsdata
            }

            grensesnittavstemmingDao.opprettAvstemming(
                avstemming.copy(avstemmingsdata = sendtAvstemmingsdata.joinToString("\n"))
            )

            logger.info(
                "Avstemming fra $periodeFraOgMed til $periodeTil fullført - ${utbetalinger.size} oppdrag ble avstemt"
            )
        } else {
            logger.warn("Utfører ikke avstemming fordi denne perioden allerede er kjørt")
        }
    }

    private fun hentTilTidStartAvDagen(): Tidspunkt =
        Tidspunkt.now(clock)
            .toZonedNorskTid()
            .truncatedTo(ChronoUnit.DAYS) // 00.00 norsk tid
            .toInstant().let {
                Tidspunkt(it)
            }

    private fun hentFraTidForrigeAvstemming() =
        grensesnittavstemmingDao.hentSisteAvstemming()?.periodeTil ?: MIN_INSTANT

    companion object {
        private val MIN_INSTANT = Tidspunkt(Instant.EPOCH)
        private val logger = LoggerFactory.getLogger(AvstemmingsdataSender::class.java)
    }

}