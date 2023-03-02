package no.nav.etterlatte.utbetaling.grensesnittavstemming

import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.utbetaling.common.tidspunktMidnattIdag
import no.nav.etterlatte.utbetaling.grensesnittavstemming.avstemmingsdata.AvstemmingsdataSender
import no.nav.etterlatte.utbetaling.grensesnittavstemming.avstemmingsdata.GrensesnittavstemmingDataMapper
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Saktype
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingDao
import org.slf4j.LoggerFactory
import java.time.Clock

class GrensesnittsavstemmingService(
    private val avstemmingsdataSender: AvstemmingsdataSender,
    private val avstemmingDao: AvstemmingDao,
    private val utbetalingDao: UtbetalingDao,
    private val clock: Clock
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun hentNestePeriode(saktype: Saktype) = Avstemmingsperiode(
        fraOgMed = avstemmingDao.hentSisteGrensesnittavstemming(saktype)?.periode?.til ?: MIN_INSTANT,
        til = tidspunktMidnattIdag(clock)
    )

    fun startGrensesnittsavstemming(
        saktype: Saktype,
        periode: Avstemmingsperiode = hentNestePeriode(saktype)
    ) {
        logger.info("Grensesnittavstemmer fra ${periode.fraOgMed} til ${periode.til}")
        val utbetalinger =
            utbetalingDao.hentUtbetalingerForGrensesnittavstemming(periode.fraOgMed, periode.til, saktype)
        val avstemmingId = UUIDBase64()

        val grensesnittavstemmingDataMapper =
            GrensesnittavstemmingDataMapper(utbetalinger, periode.fraOgMed, periode.til, avstemmingId)
        val avstemmingsdataListe = grensesnittavstemmingDataMapper.opprettAvstemmingsmelding()

        val sendtAvstemmingsdata = avstemmingsdataListe.mapIndexed { index, avstemmingsdata ->
            val sendtAvstemmingsdata = avstemmingsdataSender.sendGrensesnittavstemming(avstemmingsdata)
            logger.info(
                "Grensesnittavstemmingsmelding ${index + 1} av ${avstemmingsdataListe.size} overført til Oppdrag"
            )
            sendtAvstemmingsdata
        }

        avstemmingDao.opprettGrensesnittavstemming(
            Grensesnittavstemming(
                id = avstemmingId,
                periode = periode,
                antallOppdrag = utbetalinger.size,
                opprettet = Tidspunkt.now(clock),
                avstemmingsdata = sendtAvstemmingsdata.joinToString("\n"),
                saktype = saktype
            )
        )

        logger.info(
            "Grensesnittsvstemming fra ${periode.fraOgMed} til ${periode.til} fullført" +
                " - ${utbetalinger.size} oppdrag ble avstemt"
        )
    }

    companion object {
        private val MIN_INSTANT = Tidspunkt.min()
    }
}