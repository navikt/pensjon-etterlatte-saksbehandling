package no.nav.etterlatte.behandling.jobs

import no.nav.etterlatte.Context
import no.nav.etterlatte.Self
import no.nav.etterlatte.behandling.doedshendelse.DoedshendelseReminderService
import no.nav.etterlatte.common.DatabaseContext
import no.nav.etterlatte.jobs.LoggerInfo
import no.nav.etterlatte.jobs.fixedRateCancellableTimer
import no.nav.etterlatte.libs.common.OpeningHours
import no.nav.etterlatte.libs.common.TimerJob
import no.nav.etterlatte.sak.SakTilgangDao
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.Timer
import javax.sql.DataSource

class DoedsmeldingReminderJob(
    private val doedshendelseReminderService: DoedshendelseReminderService,
    private val erLeader: () -> Boolean,
    private val initialDelay: Long,
    private val interval: Duration,
    dataSource: DataSource,
    val sakTilgangDao: SakTilgangDao,
    private val openingHours: OpeningHours,
) : TimerJob {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val jobbNavn = this::class.simpleName

    private var jobContext: Context =
        Context(Self(doedshendelseReminderService::class.java.simpleName), DatabaseContext(dataSource), sakTilgangDao, null)

    override fun schedule(): Timer {
        logger.info("$jobbNavn er satt til å kjøre med ${doedshendelseReminderService::class.simpleName} og periode $interval")

        return fixedRateCancellableTimer(
            name = jobbNavn,
            initialDelay = initialDelay,
            loggerInfo = LoggerInfo(logger = logger, loggTilSikkerLogg = false),
            period = interval.toMillis(),
            openingHours = openingHours,
        ) {
            if (erLeader()) {
                doedshendelseReminderService.setupKontekstAndRun(jobContext)
            }
        }
    }
}
