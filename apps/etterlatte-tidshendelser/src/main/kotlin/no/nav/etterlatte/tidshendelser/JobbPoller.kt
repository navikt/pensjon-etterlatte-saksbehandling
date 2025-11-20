package no.nav.etterlatte.tidshendelser

import no.nav.etterlatte.jobs.LoggerInfo
import no.nav.etterlatte.jobs.fixedRateCancellableTimer
import no.nav.etterlatte.libs.common.OpeningHours
import no.nav.etterlatte.libs.common.TimerJob
import no.nav.etterlatte.libs.tidshendelser.JobbKategori
import no.nav.etterlatte.tidshendelser.aarliginntektsjustering.AarligInntektsjusteringService
import no.nav.etterlatte.tidshendelser.aldersovergang.AldersovergangerService
import no.nav.etterlatte.tidshendelser.etteroppgjoer.EtteroppgjoerService
import no.nav.etterlatte.tidshendelser.hendelser.HendelseDao
import no.nav.etterlatte.tidshendelser.omregning.ReguleringService
import no.nav.etterlatte.tidshendelser.omstillingsstoenad.OmstillingsstoenadService
import no.nav.etterlatte.tidshendelser.oppgave.OppdaterSkjermingBpService
import no.nav.etterlatte.tidshendelser.oppgave.OppfoelgingBpFylt18Service
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.Timer

class JobbPollerTask(
    private val initialDelaySeconds: Long,
    private val periode: Duration,
    private val openingHours: OpeningHours,
    private val jobbPoller: JobbPoller,
) : TimerJob {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun schedule(): Timer {
        logger.info("Starter polling av jobber pr $periode")

        return fixedRateCancellableTimer(
            name = "JOBB_POLLER_TASK",
            initialDelay = initialDelaySeconds * 1000,
            period = periode.toMillis(),
            loggerInfo = LoggerInfo(logger = logger),
            openingHours = openingHours,
        ) {
            jobbPoller.poll()
        }
    }
}

class JobbPoller(
    private val hendelseDao: HendelseDao,
    private val aldersovergangerService: AldersovergangerService,
    private val omstillingsstoenadService: OmstillingsstoenadService,
    private val reguleringService: ReguleringService,
    private val inntektsjusteringService: AarligInntektsjusteringService,
    private val oppfoelgingBpFylt18Service: OppfoelgingBpFylt18Service,
    private val oppdaterSkjermingBpService: OppdaterSkjermingBpService,
    private val etteroppgjoerService: EtteroppgjoerService,
) {
    private val logger = LoggerFactory.getLogger(JobbPoller::class.java)

    fun poll() {
        logger.info("Sjekker for jobber å starte...")

        hendelseDao.finnAktuellJobb().forEach {
            try {
                logger.info("Fant jobb ${it.id}, type=${it.type}, status=${it.status}")
                hendelseDao.oppdaterJobbstatusStartet(it)

                val saker =
                    when (it.type.kategori) {
                        JobbKategori.ALDERSOVERGANG -> aldersovergangerService.execute(it)
                        JobbKategori.OMS_DOEDSDATO -> omstillingsstoenadService.execute(it)
                        JobbKategori.REGULERING -> reguleringService.execute(it)
                        JobbKategori.AARLIG_INNTEKTSJUSTERING -> inntektsjusteringService.execute(it)
                        JobbKategori.OPPFOELGING_BP_FYLT_18 -> oppfoelgingBpFylt18Service.execute(it)
                        JobbKategori.OPPDATERING_SKJERMING_BP -> oppdaterSkjermingBpService.execute(it)
                        JobbKategori.OPPRETT_ETTEROPPGJOER_FORBEHANDLING -> etteroppgjoerService.execute(it)
                    }

                if (saker.isEmpty()) {
                    // Nuttin' to do
                    val jobbRefreshed = hendelseDao.hentJobb(it.id)
                    hendelseDao.oppdaterJobbstatusFerdig(jobbRefreshed)
                }
            } catch (outer: Exception) {
                try {
                    hendelseDao.tilbakestillJobSomIkkeStartetSkikkelig(it.id)
                    logger.error(
                        "Kjøring av jobb=${it.type} med id=${it.id} feilet. Statusen er tilbakestilt og vil " +
                            "bli forsøkt på nytt, men hvis denne ikke går igjennom i løpet av dagen må ny kjøring " +
                            "settes opp for denne jobben",
                        outer,
                    )
                } catch (inner: Exception) {
                    logger.error("Kunne ikke tilbakestille kjøringen av jobb ${it.type}.", inner)
                    logger.error(
                        "Kjøring av jobb=${it.type} med id=${it.id} feilet. Vi kunne heller " +
                            "ikke tilbakestille kjøringen av jobben (se feil over). Dette betyr at denne jobben " +
                            "MÅ manuelt legges inn på nytt / oppdateres for å få kjørt den.",
                        outer,
                    )
                }
            }
        }
    }
}
