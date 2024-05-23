package no.nav.etterlatte.behandling.jobs

import no.nav.etterlatte.Context
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.Self
import no.nav.etterlatte.common.DatabaseContext
import no.nav.etterlatte.jobs.LoggerInfo
import no.nav.etterlatte.jobs.fixedRateCancellableTimer
import no.nav.etterlatte.libs.common.TimerJob
import no.nav.etterlatte.oppgave.FristGaarUtJobService
import no.nav.etterlatte.sak.SakTilgangDao
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.Date
import java.util.Timer
import javax.sql.DataSource

class FristGaarUtJobb(
    private val erLeader: () -> Boolean,
    private val starttidspunkt: Date,
    private val periode: Duration,
    private val fristGaarUtJobService: FristGaarUtJobService,
    dataSource: DataSource,
    sakTilgangDao: SakTilgangDao,
) : TimerJob {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val jobbNavn = this::class.simpleName

    private var jobContext: Context =
        Context(Self(fristGaarUtJobService::class.java.simpleName), DatabaseContext(dataSource), sakTilgangDao)

    override fun schedule(): Timer {
        logger.debug("{} er satt til å starte {} med periode {}", jobbNavn, starttidspunkt, periode)

        return fixedRateCancellableTimer(
            name = jobbNavn,
            startAt = starttidspunkt,
            period = periode.toMillis(),
            loggerInfo = LoggerInfo(logger = logger, loggTilSikkerLogg = false),
        ) {
            if (erLeader()) {
                Kontekst.set(jobContext)
                fristGaarUtJobService.setupKontekstAndRun(jobContext)
            }
        }
    }
}
