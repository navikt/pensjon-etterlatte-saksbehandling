package no.nav.etterlatte.oppgave

import no.nav.etterlatte.Context
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.Self
import no.nav.etterlatte.common.DatabaseContext
import no.nav.etterlatte.jobs.LoggerInfo
import no.nav.etterlatte.jobs.fixedRateCancellableTimer
import no.nav.etterlatte.libs.common.TimerJob
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.oppgave.VentefristGaarUtRequest
import no.nav.etterlatte.sak.SakTilgangDao
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDate
import java.util.Date
import java.util.Timer
import javax.sql.DataSource

class FristGaarUtJobb(
    private val erLeader: () -> Boolean,
    private val starttidspunkt: Date,
    private val periode: Duration,
    private val service: OppgaveService,
    dataSource: DataSource,
    sakTilgangDao: SakTilgangDao,
) : TimerJob {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val jobbNavn = this::class.simpleName

    private var jobContext: Context = Context(Self(service::class.java.simpleName), DatabaseContext(dataSource), sakTilgangDao)

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

                val request =
                    VentefristGaarUtRequest(
                        dato = LocalDate.now(),
                        type = (OppgaveType.entries - OppgaveType.GJENOPPRETTING_ALDERSOVERGANG),
                        oppgaveKilde = (OppgaveKilde.entries - OppgaveKilde.GJENOPPRETTING),
                        oppgaver = listOf(),
                    )
                service.hentFristGaarUt(request).forEach {
                    logger.info("Frist er gått ut for ${it.oppgaveID}, tar av vent")
                    try {
                        service.oppdaterStatusOgMerknad(
                            it.oppgaveID,
                            it.merknad ?: "",
                            Status.UNDER_BEHANDLING,
                        )
                    } catch (e: ForespoerselException) {
                        logger.warn("Klarte ikke ta oppgave ${it.oppgaveID} av vent. Fortsetter med neste", e)
                    }
                    logger.info("Tok ${it.oppgaveID} av vent")
                }
                logger.info("Ferdig med å ta oppgaver av vent")
            }
        }
    }
}
