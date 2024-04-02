package no.nav.etterlatte.oppgave

import jobs.TimerJob
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.jobs.LoggerInfo
import no.nav.etterlatte.jobs.fixedRateCancellableTimer
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.oppgave.VentefristGaarUtRequest
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDate
import java.util.Date
import java.util.Timer

class FristGaarUtJobb(
    private val erLeader: () -> Boolean,
    private val starttidspunkt: Date,
    private val periode: Duration,
    private val service: OppgaveService,
    private val featureToggleService: FeatureToggleService,
) : TimerJob {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val jobbNavn = this::class.simpleName

    override fun schedule(): Timer {
        logger.debug("{} er satt til å starte {} med periode {}", jobbNavn, starttidspunkt, periode)

        return fixedRateCancellableTimer(
            name = jobbNavn,
            startAt = starttidspunkt,
            period = periode.toMillis(),
            loggerInfo = LoggerInfo(logger = logger, loggTilSikkerLogg = false),
        ) {
            if (erLeader()) {
                val request =
                    VentefristGaarUtRequest(
                        dato = LocalDate.now(),
                        type = (OppgaveType.entries - OppgaveType.GJENOPPRETTING_ALDERSOVERGANG),
                        oppgaveKilde = (OppgaveKilde.entries - OppgaveKilde.GJENOPPRETTING),
                        oppgaver = listOf(),
                    )
                val automatiskAvskrivFrist = featureToggleService.isEnabled(FristFeatureToggle.AutomatiskAvskrivFrist, false)
                service.hentFristGaarUt(request).forEach {
                    if (automatiskAvskrivFrist) {
                        logger.info("Frist er gått ut for ${it.oppgaveID}, tar av vent")
                        service.oppdaterStatusOgMerknad(
                            it.oppgaveID,
                            it.merknad ?: "",
                            Status.UNDER_BEHANDLING,
                        )
                        logger.info("Tok ${it.oppgaveID} av vent")
                    } else {
                        logger.debug(
                            "Automatisk avskriv frist er skrudd av, gjør derfor ingenting for {}",
                            it.oppgaveID,
                        )
                    }
                }
                logger.info("Ferdig med å ta oppgaver av vent")
            }
        }
    }
}

enum class FristFeatureToggle(private val key: String) : FeatureToggle {
    AutomatiskAvskrivFrist("automatisk-avskriv-frist"),
    ;

    override fun key() = key
}
