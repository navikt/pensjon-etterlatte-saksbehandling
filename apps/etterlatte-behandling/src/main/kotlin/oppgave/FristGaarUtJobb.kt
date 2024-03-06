package no.nav.etterlatte.oppgave

import no.nav.etterlatte.jobs.LoggerInfo
import no.nav.etterlatte.jobs.TimerJob
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
) : TimerJob {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val jobbNavn = this::class.simpleName

    override fun schedule(): Timer {
        logger.info("$jobbNavn er satt til å starte $starttidspunkt med periode $periode")

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
                service.hentFristGaarUt(request).forEach {
                    service.oppdaterStatusOgMerknad(
                        it.oppgaveID,
                        it.merknad ?: "",
                        Status.UNDER_BEHANDLING,
                    )
                }
            }
        }
    }
}
