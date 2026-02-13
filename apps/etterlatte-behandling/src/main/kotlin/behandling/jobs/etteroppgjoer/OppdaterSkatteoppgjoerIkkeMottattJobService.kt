package no.nav.etterlatte.behandling.jobs.etteroppgjoer

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.Context
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.etteroppgjoer.ETTEROPPGJOER_AAR
import no.nav.etterlatte.behandling.etteroppgjoer.Etteroppgjoer
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerService
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerStatus
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerToggles
import no.nav.etterlatte.behandling.etteroppgjoer.oppgave.EtteroppgjoerOppgaveService
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.sak.SakId
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.Month

class OppdaterSkatteoppgjoerIkkeMottattJobService(
    private val featureToggleService: FeatureToggleService,
    private val etteroppgjoerOppgaveService: EtteroppgjoerOppgaveService,
    private val etteroppgjoerService: EtteroppgjoerService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun startKjoering(jobContext: Context) {
        Kontekst.set(jobContext)

        // Alle skal ha mottatt skatteoppgjør i Desember
        if (LocalDate.now().month != Month.DECEMBER) {
            logger.info("Periodisk jobb for å oppdatere saker med skatteoppgjør ikke mottatt kan kun kjøres i desember")
            return
        }

        if (!featureToggleService.isEnabled(EtteroppgjoerToggles.OPPDATER_SKATTEOPPGJOER_IKKE_MOTTATT, false)) {
            logger.info("Periodisk jobb for å oppdatere saker med skatteoppgjør ikke mottatt er deaktivert")
            return
        }

        logger.info("Periodisk jobb for å oppdatere saker med ikke mottatt skatteoppgjør og opprette forbehandlinger")
        runBlocking {
            oppdaterSkatteoppgjoerIkkeMottatt()
        }
    }

    fun oppdaterSkatteoppgjoerIkkeMottatt() {
        val relevanteEtteroppgjoer: List<Etteroppgjoer> =
            inTransaction {
                etteroppgjoerService.hentEtteroppgjoerSakerSomVenterPaaSkatteoppgjoer(
                    antall = 200,
                )
            }

        relevanteEtteroppgjoer.map { etteroppgjoer ->
            try {
                inTransaction {
                    etteroppgjoerService.oppdaterEtteroppgjoerStatus(
                        etteroppgjoer.sakId,
                        ETTEROPPGJOER_AAR,
                        EtteroppgjoerStatus.MANGLER_SKATTEOPPGJOER,
                    )
                    etteroppgjoerOppgaveService.opprettOppgaveForOpprettForbehandling(
                        sakId = etteroppgjoer.sakId,
                        inntektsAar = ETTEROPPGJOER_AAR,
                    )
                }
            } catch (e: Error) {
                logger.error(
                    "Kunne ikke opprette etteroppgjør forbehandling for sak med id: ${etteroppgjoer.sakId} for inntektsaar ${etteroppgjoer.inntektsaar}",
                    e,
                )
            }
        }

        logger.info(
            "Opprettet ${relevanteEtteroppgjoer.size} oppgaver for opprettelse av forbehandling hvor vi ikke har mottatt skatteoppgjør",
        )
    }
}
