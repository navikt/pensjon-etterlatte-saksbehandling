package no.nav.etterlatte.behandling.jobs

import no.nav.etterlatte.Context
import no.nav.etterlatte.Self
import no.nav.etterlatte.behandling.omregning.OmregningKlassifikasjonskodeJobService
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

class OmregningKlassifikasjonskodeJob(
    private val erLeader: () -> Boolean,
    private val initialDelay: Long,
    private val interval: Duration,
    private val omregningKlassifikasjonskodeJobService: OmregningKlassifikasjonskodeJobService,
    dataSource: DataSource,
    sakTilgangDao: SakTilgangDao,
) : TimerJob {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val jobbNavn = this::class.simpleName

    private var jobContext: Context =
        Context(
            Self(omregningKlassifikasjonskodeJobService::class.java.simpleName),
            DatabaseContext(dataSource),
            sakTilgangDao,
            HardkodaSystembruker.oppgave,
        )

    override fun schedule(): Timer {
        logger.info("$jobbNavn er satt til å kjøre med ${omregningKlassifikasjonskodeJobService::class.simpleName} og periode $interval")

        return fixedRateCancellableTimer(
            name = jobbNavn,
            initialDelay = initialDelay,
            loggerInfo = LoggerInfo(logger = logger, loggTilSikkerLogg = false),
            period = interval.toMillis(),
        ) {
            if (erLeader()) {
                omregningKlassifikasjonskodeJobService.setupKontekstAndRun(jobContext)
            }
        }
    }
}
