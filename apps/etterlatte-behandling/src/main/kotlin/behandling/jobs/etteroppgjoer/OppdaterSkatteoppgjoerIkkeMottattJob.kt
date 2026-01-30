package no.nav.etterlatte.behandling.jobs.etteroppgjoer

import no.nav.etterlatte.Context
import no.nav.etterlatte.Self
import no.nav.etterlatte.common.DatabaseContext
import no.nav.etterlatte.jobs.LoggerInfo
import no.nav.etterlatte.jobs.fixedRateCancellableTimer
import no.nav.etterlatte.libs.common.TimerJob
import no.nav.etterlatte.libs.ktor.token.HardkodaSystembruker
import no.nav.etterlatte.sak.SakTilgangDao
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.Timer
import javax.sql.DataSource

class OppdaterSkatteoppgjoerIkkeMottattJob(
    private val oppdaterSkatteoppgjoerIkkeMottattJobService: OppdaterSkatteoppgjoerIkkeMottattJobService,
    private val erLeader: () -> Boolean,
    private val initialDelay: Long,
    private val interval: Duration,
    private val dataSource: DataSource,
    private val sakTilgangDao: SakTilgangDao,
) : TimerJob {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val jobbNavn = this::class.simpleName

    private var jobContext: Context =
        Context(
            Self(OppdaterSkatteoppgjoerIkkeMottattJob::class.java.simpleName),
            DatabaseContext(dataSource),
            sakTilgangDao,
            HardkodaSystembruker.etteroppgjoer,
        )

    override fun schedule(): Timer {
        logger.info("$jobbNavn er satt til å kjøre med periode $interval")

        return fixedRateCancellableTimer(
            name = jobbNavn,
            initialDelay = initialDelay,
            loggerInfo = LoggerInfo(logger = logger, loggTilSikkerLogg = false),
            period = interval.toMillis(),
        ) {
            if (erLeader()) {
                oppdaterSkatteoppgjoerIkkeMottattJobService.startKjoering(jobContext)
            }
        }
    }
}
