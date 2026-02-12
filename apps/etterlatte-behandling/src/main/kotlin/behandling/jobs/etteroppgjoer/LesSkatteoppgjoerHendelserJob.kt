package behandling.jobs.etteroppgjoer

import kotlinx.coroutines.sync.Semaphore
import no.nav.etterlatte.Context
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.Self
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerToggles
import no.nav.etterlatte.behandling.jobs.etteroppgjoer.HendelseKjoeringRequest
import no.nav.etterlatte.behandling.jobs.etteroppgjoer.SkatteoppgjoerHendelserService
import no.nav.etterlatte.common.DatabaseContext
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.jobs.LoggerInfo
import no.nav.etterlatte.jobs.fixedRateCancellableTimer
import no.nav.etterlatte.libs.common.TimerJob
import no.nav.etterlatte.libs.ktor.token.HardkodaSystembruker
import no.nav.etterlatte.sak.SakTilgangDao
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.Timer
import javax.sql.DataSource

class LesSkatteoppgjoerHendelserJob(
    private val skatteoppgjoerHendelserService: SkatteoppgjoerHendelserService,
    private val erLeader: () -> Boolean,
    private val initialDelay: Long,
    private val interval: Duration,
    private val hendelserBatchSize: Int,
    private val sakTilgangDao: SakTilgangDao,
    private val featureToggleService: FeatureToggleService,
    dataSource: DataSource,
) : TimerJob {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val jobbNavn = this::class.simpleName
    private val lock = Semaphore(1, 0)
    private val antallKjoeringer = 100

    private var jobContext: Context =
        Context(
            AppUser = Self(skatteoppgjoerHendelserService::class.java.simpleName),
            databasecontxt = DatabaseContext(dataSource),
            sakTilgangDao = sakTilgangDao,
            brukerTokenInfo = HardkodaSystembruker.etteroppgjoer,
        )

    override fun schedule(): Timer {
        if (jobbenErAktivert()) {
            logger.info(
                "$jobbNavn er satt til å kjøre med skatteoppgjoerHendelserService=${skatteoppgjoerHendelserService::class.simpleName} og periode $interval",
            )
        }

        return fixedRateCancellableTimer(
            name = jobbNavn,
            initialDelay = initialDelay,
            loggerInfo = LoggerInfo(logger = logger, loggTilSikkerLogg = false),
            period = interval.toMillis(),
        ) {
            if (erLeader() && jobbenErAktivert()) {
                if (lock.tryAcquire()) {
                    Kontekst.set(jobContext)

                    try {
                        lesOgBehandleFlereGanger()
                    } catch (e: Exception) {
                        logger.error("Feilet under lesing og behandling av hendelser fra Skatt", e)
                    }
                    lock.release()
                } else {
                    logger.info("Jobben kjører allerede, vi starter ikke en ny kjøring")
                }
            }
        }
    }

    private fun lesOgBehandleFlereGanger() {
        logger.info("Leser og behandler $hendelserBatchSize hendelser fra skatt ")
        if (jobbenErAktivert()) {
            skatteoppgjoerHendelserService.lesOgBehandleHendelser(
                HendelseKjoeringRequest(hendelserBatchSize, ETTEROPPGJOER_AAR, true),
            )
        } else {
            logger.info("Avslutter fordi feature toggle er av")
        }
    }

    private fun jobbenErAktivert(): Boolean = featureToggleService.isEnabled(EtteroppgjoerToggles.ETTEROPPGJOER_SKATTEHENDELSES_JOBB, false)
}
