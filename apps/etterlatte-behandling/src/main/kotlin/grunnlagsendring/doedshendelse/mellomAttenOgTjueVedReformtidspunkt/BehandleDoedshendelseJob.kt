package no.nav.etterlatte.grunnlagsendring.doedshendelse.mellom18og20PaaReformtidspunkt

import no.nav.etterlatte.Context
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.Self
import no.nav.etterlatte.common.DatabaseContext
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.grunnlagsendring.doedshendelse.DoedshendelseDao
import no.nav.etterlatte.grunnlagsendring.doedshendelse.Status
import no.nav.etterlatte.grunnlagsendring.doedshendelse.mellomAttenOgTjueVedReformtidspunkt.MellomAttenOgTjueVedReformtidspunktFeatureToggle
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.jobs.LoggerInfo
import no.nav.etterlatte.jobs.fixedRateCancellableTimer
import no.nav.etterlatte.libs.common.TimerJob
import no.nav.etterlatte.libs.common.person.maskerFnr
import no.nav.etterlatte.sak.SakTilgangDao
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.Timer
import javax.sql.DataSource

class BehandleDoedshendelseJob(
    private val doedshendelseDao: DoedshendelseDao,
    private val behandleDoedshendelseService: BehandleDoedshendelseService,
    private val featureToggleService: FeatureToggleService,
    private val erLeader: () -> Boolean,
    private val initialDelay: Long,
    private val interval: Duration,
    dataSource: DataSource,
    val sakTilgangDao: SakTilgangDao,
) : TimerJob {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val jobbNavn = this::class.simpleName

    private var jobContext: Context = Context(Self(this::class.java.simpleName), DatabaseContext(dataSource), sakTilgangDao, null)

    override fun schedule(): Timer {
        logger.info("$jobbNavn er satt til å kjøre med periode $interval")

        return fixedRateCancellableTimer(
            name = jobbNavn,
            initialDelay = initialDelay,
            loggerInfo = LoggerInfo(logger = logger, loggTilSikkerLogg = false),
            period = interval.toMillis(),
        ) {
            if (erLeader()) {
                Kontekst.set(jobContext)
                run()
            }
        }
    }

    private fun run() {
        if (featureToggleService.isEnabled(MellomAttenOgTjueVedReformtidspunktFeatureToggle.KanLagreDoedshendelse, false)) {
            val doedshendelserSomSkalHaanderes = inTransaction { hentAlleNyeDoedsmeldinger() }
            logger.info("Antall nye dødsmeldinger ${doedshendelserSomSkalHaanderes.size}")

            doedshendelserSomSkalHaanderes.forEach { doedshendelse ->
                inTransaction {
                    logger.info("Starter håndtering av dødshendelse for person ${doedshendelse.beroertFnr.maskerFnr()}")
                    behandleDoedshendelseService.haandterDoedshendelse(doedshendelse)
                }
            }
        }
    }

    private fun hentAlleNyeDoedsmeldinger() =
        doedshendelseDao.hentDoedshendelserMedStatus(
            listOf(Status.NY, Status.OPPDATERT),
            migrertMellomAttenOgTjue = true,
        )
}
