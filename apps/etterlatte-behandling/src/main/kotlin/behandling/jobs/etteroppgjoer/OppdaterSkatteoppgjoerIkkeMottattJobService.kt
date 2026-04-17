package no.nav.etterlatte.behandling.jobs.etteroppgjoer

import no.nav.etterlatte.Context
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerService
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerToggles
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.inTransaction
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.Month

/**
 * Oppretter etteroppgjør når skatteoppgjør ikke har kommet inn via normal hendelsesflyt i [LesSkatteoppgjoerHendelserJobService].
 *
 * Alle skal ha mottatt skatteoppgjør innen 1. Desember. For de sakene som ikke har mottatt skatteoppgjør skal vi alikevell opprette
 * etteroppgjør og oppgave for opprettelse av forbehandling, slik at de kommer med i det ordinære etteroppgjørsarbeidet.
 *
 * TL;DR Jobbe skal kun kjøres etter 1. Desember for å fange opp manglende etteroppgjør
 */
class OppdaterSkatteoppgjoerIkkeMottattJobService(
    private val featureToggleService: FeatureToggleService,
    private val etteroppgjoerService: EtteroppgjoerService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun startKjoering(jobContext: Context) {
        Kontekst.set(jobContext)

        if (LocalDate.now().month != Month.DECEMBER) {
            logger.info("Periodisk jobb for å oppdatere saker med skatteoppgjør ikke mottatt kan kun kjøres i desember")
            return
        }

        if (!featureToggleService.isEnabled(EtteroppgjoerToggles.OPPDATER_SKATTEOPPGJOER_IKKE_MOTTATT, false)) {
            logger.info("Periodisk jobb for å oppdatere saker med skatteoppgjør ikke mottatt er deaktivert")
            return
        }

        // Vi gjør alltid etteroppgjør for forrige år
        val etteroppgjoersAar = LocalDate.now().year - 1

        logger.info("Starter jobb for å oppdatere saker med skatteoppgjør ikke mottatt")
        etteroppgjoerService.finnOgOpprettManglendeEtteroppgjoerForAar(etteroppgjoersAar)
        oppdaterEtteroppgjoerIkkeMottatt()
    }

    private fun oppdaterEtteroppgjoerIkkeMottatt() {
        val relevanteEtteroppgjoer =
            inTransaction {
                etteroppgjoerService.hentEtteroppgjoerSomVenterPaaSkatteoppgjoer(antall = 200)
            }

        relevanteEtteroppgjoer.forEach { etteroppgjoer ->
            try {
                inTransaction { etteroppgjoerService.haandterSkatteoppgjoerIkkeMottatt(etteroppgjoer) }
            } catch (e: Exception) {
                logger.error(
                    "Kunne ikke håndtere manglende skatteoppgjør for sak=${etteroppgjoer.sakId}, inntektsaar=${etteroppgjoer.inntektsaar}",
                    e,
                )
            }
        }

        logger.info(
            "Opprettet ${relevanteEtteroppgjoer.size} oppgaver for opprettelse av forbehandling hvor vi ikke har mottatt skatteoppgjør",
        )
    }
}
