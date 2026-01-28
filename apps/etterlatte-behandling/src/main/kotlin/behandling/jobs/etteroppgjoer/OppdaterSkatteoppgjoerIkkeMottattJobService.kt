package no.nav.etterlatte.behandling.jobs.etteroppgjoer

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.Context
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.etteroppgjoer.ETTEROPPGJOER_AAR
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerService
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerStatus
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerToggles
import no.nav.etterlatte.behandling.etteroppgjoer.oppgave.EtteroppgjoerOppgaveService
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.EtteroppgjoerFilter
import no.nav.etterlatte.libs.common.sak.SakId
import org.slf4j.LoggerFactory
import java.time.LocalDate

class OppdaterSkatteoppgjoerIkkeMottattJobService(
    private val featureToggleService: FeatureToggleService,
    private val etteroppgjoerOppgaveService: EtteroppgjoerOppgaveService,
    private val etteroppgjoerService: EtteroppgjoerService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun startKjoering(jobContext: Context) {
        Kontekst.set(jobContext)

        if (!featureToggleService.isEnabled(EtteroppgjoerToggles.OPPDATER_SKATTEOPPGJOER_IKKE_MOTTATT, false)) {
            logger.info("Periodisk jobb for å oppdatere saker med skatteoppgjør ikke mottatt er deaktivert")
            return
        }

        logger.info("Oppdatere saker med skatteoppgjør som ikke er mottatt")
        runBlocking {
            oppdaterSkatteoppgjoerIkkeMottatt()
        }
    }

    fun oppdaterSkatteoppgjoerIkkeMottatt() {
        val relevanteSaker: List<SakId> =
            etteroppgjoerService.hentEtteroppgjoerSakerIBulk(
                inntektsaar = ETTEROPPGJOER_AAR,
                antall = 200,
                etteroppgjoerFilter = EtteroppgjoerFilter.ALLE_SAKER,
                status = EtteroppgjoerStatus.VENTER_PAA_SKATTEOPPGJOER,
            )

        relevanteSaker.map { sakId ->
            try {
                etteroppgjoerService.oppdaterEtteroppgjoerStatus(sakId, ETTEROPPGJOER_AAR, EtteroppgjoerStatus.MANGLER_SKATTEOPPGJOER)
                etteroppgjoerOppgaveService.opprettOppgaveForOpprettForbehandling(sakId)
            } catch (e: Error) {
                logger.error("Kunne ikke opprette etteroppgjør forbehandling for sak med id: $sakId", e)
            }
        }
    }
}
