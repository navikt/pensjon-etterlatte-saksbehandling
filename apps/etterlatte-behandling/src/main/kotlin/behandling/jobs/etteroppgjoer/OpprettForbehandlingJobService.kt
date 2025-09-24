package no.nav.etterlatte.behandling.jobs.etteroppgjoer

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.Context
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerService
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerStatus
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerToggles
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.oppgave.OppgaveService
import org.slf4j.LoggerFactory
import java.time.YearMonth

class OpprettForbehandlingJobService(
    private val etteroppgjoerService: EtteroppgjoerService,
    private val oppgaveService: OppgaveService,
    private val featureToggleService: FeatureToggleService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun startKjoering(jobContext: Context) {
        Kontekst.set(jobContext)
        if (featureToggleService.isEnabled(EtteroppgjoerToggles.ETTEROPPGJOER_OPPRETT_FORBEHANDLING_JOBB, false)) {
            logger.info("Starter periodiske jobber for å opprette forbehandlinger")
            runBlocking {
                opprettOppgaveForForbehandlinger()
            }
        } else {
            logger.info("Periodisk jobber for å opprette forbehandlinger er deaktivert")
        }
    }

    fun opprettOppgaveForForbehandlinger() {
        // TODO: global param?
        val etteroppgjoersAar = YearMonth.now().year - 1
        val etteroppgjoer = etteroppgjoerService.hentEtteroppgjoerForStatus(EtteroppgjoerStatus.MOTTATT_SKATTEOPPGJOER, etteroppgjoersAar)

        val antall =
            etteroppgjoer.count { etteroppgjoer ->
                val eksisterendeOppgaver = oppgaveService.hentOppgaverForSakAvType(etteroppgjoer.sakId, listOf(OppgaveType.ETTEROPPGJOER))

                if (eksisterendeOppgaver.isEmpty()) {
                    oppgaveService.opprettOppgave(
                        referanse = "", // viktig for å få opp modal for opprette forbehandling
                        sakId = etteroppgjoer.sakId,
                        kilde = OppgaveKilde.HENDELSE,
                        type = OppgaveType.ETTEROPPGJOER,
                        merknad = "Etteroppgjøret er nå klar til å behandles",
                    )
                    true
                }
                false
            }

        logger.info("Oppretta $antall oppgaver for forbehandling for Etteroppgjøret $etteroppgjoersAar")
    }
}
