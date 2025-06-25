package no.nav.etterlatte.behandling.jobs.aktivitetsplikt

import no.nav.etterlatte.jobs.LoggerInfo
import no.nav.etterlatte.jobs.fixedRateCancellableTimer
import no.nav.etterlatte.libs.common.TimerJob
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.Timer

class AktivitetspliktOppgaveUnntakUtloeperJob(
    private val aktivitetspliktOppgaveUnntakUtloeperJobService: AktivitetspliktOppgaveUnntakUtloeperJobService,
    private val erLeader: () -> Boolean,
    private val initialDelay: Long,
    private val interval: Duration,
) : TimerJob {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val jobbNavn = this::class.simpleName

    override fun schedule(): Timer {
        logger.info("Starter jobb $jobbNavn med interval $interval")

        return fixedRateCancellableTimer(
            name = jobbNavn,
            initialDelay = initialDelay,
            loggerInfo = LoggerInfo(logger = logger, loggTilSikkerLogg = false),
            period = interval.toMillis(),
        ) {
            if (erLeader()) {
                aktivitetspliktOppgaveUnntakUtloeperJobService.run()
            }
        }
    }
}
