package no.nav.etterlatte.oppgave

import no.nav.etterlatte.Context
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.oppgave.VentefristGaarUtRequest
import org.slf4j.LoggerFactory
import java.time.LocalDate

class OppgaveFristGaarUtJobService(
    private val service: OppgaveService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun setupKontekstAndRun(context: Context) {
        Kontekst.set(context)
        run()
    }

    private fun run() {
        val request =
            VentefristGaarUtRequest(
                dato = LocalDate.now(),
                type = (OppgaveType.entries - OppgaveType.GJENOPPRETTING_ALDERSOVERGANG),
                oppgaveKilde = (OppgaveKilde.entries - OppgaveKilde.GJENOPPRETTING),
                oppgaver = listOf(),
            )
        inTransaction {
            service.hentFristGaarUt(request).forEach {
                logger.info("Frist er gått ut for ${it.oppgaveId}, tar av vent")
                try {
                    service.oppdaterStatusOgMerknad(
                        it.oppgaveId,
                        it.merknad ?: "",
                        Status.UNDER_BEHANDLING,
                    )
                } catch (e: ForespoerselException) {
                    logger.warn("Klarte ikke ta oppgave ${it.oppgaveId} av vent. Fortsetter med neste", e)
                }
                logger.info("Tok ${it.oppgaveId} av vent")
            }
        }
        logger.info("Ferdig med å ta oppgaver av vent")
    }
}
