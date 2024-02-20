package no.nav.etterlatte.behandling.jobs

import no.nav.etterlatte.grunnlagsendring.doedshendelse.DoedshendelseJobService
import no.nav.etterlatte.jobs.LoggerInfo
import no.nav.etterlatte.jobs.fixedRateCancellableTimer
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.Timer

class DoedsmeldingJob(
    private val doedshendelseService: DoedshendelseJobService,
    private val erLeader: () -> Boolean,
    private val initialDelay: Long,
    private val interval: Duration,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val jobbNavn = this::class.simpleName

    fun schedule(): Timer {
        logger.info("$jobbNavn er satt til å kjøre med doedshendelseService=${doedshendelseService::class.simpleName} og periode $interval")

        return fixedRateCancellableTimer(
            name = jobbNavn,
            initialDelay = initialDelay,
            loggerInfo = LoggerInfo(logger = logger, loggTilSikkerLogg = false),
            period = interval.toMillis(),
        ) {
            if (erLeader()) {
                doedshendelseService.setupKontekst().run()
            }
        }
    }
}
