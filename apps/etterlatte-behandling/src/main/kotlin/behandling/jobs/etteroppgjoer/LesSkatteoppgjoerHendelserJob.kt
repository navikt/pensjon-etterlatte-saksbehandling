package behandling.jobs.etteroppgjoer

import no.nav.etterlatte.Context
import no.nav.etterlatte.Self
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerToggles
import no.nav.etterlatte.behandling.etteroppgjoer.sigrun.HendelseKjoeringRequest
import no.nav.etterlatte.behandling.etteroppgjoer.sigrun.SkatteoppgjoerHendelserService
import no.nav.etterlatte.common.DatabaseContext
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.jobs.LoggerInfo
import no.nav.etterlatte.jobs.fixedRateCancellableTimer
import no.nav.etterlatte.libs.common.TimerJob
import no.nav.etterlatte.libs.ktor.token.HardkodaSystembruker
import no.nav.etterlatte.sak.SakTilgangDao
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDate
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

    private var jobContext: Context =
        Context(
            AppUser = Self(skatteoppgjoerHendelserService::class.java.simpleName),
            databasecontxt = DatabaseContext(dataSource),
            sakTilgangDao = sakTilgangDao,
            brukerTokenInfo = HardkodaSystembruker.etteroppgjoer,
        )

    override fun schedule(): Timer {
        if (featureToggleService.isEnabled(EtteroppgjoerToggles.ETTEROPPGJOER_SKATTEHENDELSES_JOBB, false)) {
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
            if (erLeader() && featureToggleService.isEnabled(EtteroppgjoerToggles.ETTEROPPGJOER_SKATTEHENDELSES_JOBB, false)) {
                val inntektsaar = inntektsaarListe()
                skatteoppgjoerHendelserService.setupKontekstAndRun(
                    HendelseKjoeringRequest(hendelserBatchSize, inntektsaar, true, 100),
                    jobContext,
                )
            }
        }
    }

    private fun inntektsaarListe(): List<Int> {
        val startaarOmstillingsstoenad = 2024
        val sisteInntektsaar = LocalDate.now().year - 1
        val inntektsaar =
            IntRange(
                start = (sisteInntektsaar - 3).coerceAtLeast(startaarOmstillingsstoenad),
                endInclusive = sisteInntektsaar,
            ).toList()
        return inntektsaar
    }
}
