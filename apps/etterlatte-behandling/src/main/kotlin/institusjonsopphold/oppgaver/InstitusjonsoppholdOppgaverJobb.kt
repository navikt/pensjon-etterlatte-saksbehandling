package no.nav.etterlatte.institusjonsopphold.oppgaver

import kotlinx.coroutines.sync.Semaphore
import no.nav.etterlatte.Context
import no.nav.etterlatte.Self
import no.nav.etterlatte.common.DatabaseContext
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.jobs.LoggerInfo
import no.nav.etterlatte.jobs.fixedRateCancellableTimer
import no.nav.etterlatte.libs.common.TimerJob
import no.nav.etterlatte.libs.ktor.token.HardkodaSystembruker
import no.nav.etterlatte.sak.SakTilgangDao
import org.slf4j.LoggerFactory
import java.lang.Thread.sleep
import java.time.Duration
import java.util.Timer
import javax.sql.DataSource

data class InstitusjonsoppholdOppgaverJobb(
    private val institusjonsoppholdOppgaverService: InstitusjonsoppholdOppgaverService,
    private val erLeader: () -> Boolean,
    private val initialDelay: Long,
    private val interval: Duration,
    private val dataSource: DataSource,
    private val sakTilgangDao: SakTilgangDao,
    private val featureToggleService: FeatureToggleService,
) : TimerJob {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val jobbNavn = this::class.simpleName

    private val lock = Semaphore(1, 0)

    private var jobContext: Context =
        Context(
            Self(institusjonsoppholdOppgaverService::class.java.simpleName),
            DatabaseContext(dataSource),
            sakTilgangDao,
            HardkodaSystembruker.institusjonsopphold,
        )

    override fun schedule(): Timer {
        logger.info("Starter jobb $jobbNavn med interval $interval")

        return fixedRateCancellableTimer(
            name = jobbNavn,
            initialDelay = initialDelay,
            loggerInfo = LoggerInfo(logger = logger, loggTilSikkerLogg = false),
            period = interval.toMillis(),
        ) {
            if (erLeader()) {
                if (lock.tryAcquire()) {
                    logger.info("Starter oppretting av grunnlagsendringshendelser for institusjonsopphold $jobbNavn")
                    try {
                        institusjonsoppholdOppgaverService.setupKontekstAndRun(jobContext)
                    } catch (e: Exception) {
                        logger.error("Feilet i henting av institusjonsopphold $jobbNavn", e)
                        sleep(10000)
                    } finally {
                        lock.release()
                    }
                    logger.info("Ferdig med henting av institusjonsopphold $jobbNavn")
                }
            }
        }
    }
}
