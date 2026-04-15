package no.nav.etterlatte.behandling.jobs.etteroppgjoer

import kotlinx.coroutines.DelicateCoroutinesApi
import no.nav.etterlatte.Context
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.etteroppgjoer.Etteroppgjoer
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerService
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerSvarfrist
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerToggles
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.EtteroppgjoerHendelser
import no.nav.etterlatte.libs.common.isProd
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.oppgave.OppgaveService
import org.slf4j.LoggerFactory

@OptIn(DelicateCoroutinesApi::class)
class EtteroppgjoerSvarfristUtloeptJobService(
    private val etteroppgjoerService: EtteroppgjoerService,
    private val oppgaveService: OppgaveService,
    private val featureToggleService: FeatureToggleService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private val svarfrist = if (isProd()) EtteroppgjoerSvarfrist.EN_MND else EtteroppgjoerSvarfrist.FEM_MINUTT

    fun startKjoering(jobContext: Context) {
        Kontekst.set(jobContext)

        if (!featureToggleService.isEnabled(EtteroppgjoerToggles.ETTEROPPGJOER_SVARFRISTUTLOEPT_JOBB, false)) {
            logger.info("Periodisk jobb for opprette oppgave svarfrist utløpt er deaktivert")
            return
        }

        logger.info("Starter periodisk jobb for å opprette oppgave svarfrist utløpt etteroppgjør")
        inTransaction { opprettOppgaverForSvarfristUtloept() }
    }

    private fun opprettOppgaverForSvarfristUtloept() {
        val relevanteEtteroppgjoer =
            etteroppgjoerService.hentEtteroppgjoerMedSvarfristUtloept(svarfrist)

        relevanteEtteroppgjoer.forEach { opprettOppgave(it) }

        logger.info("Behandlet ${relevanteEtteroppgjoer.size} etteroppgjør med utløpt svarfrist")
    }

    private fun opprettOppgave(etteroppgjoer: Etteroppgjoer) {
        val forbehandlingId = etteroppgjoer.sisteFerdigstilteForbehandling.toString()

        val oppgaveFinnesAllerede =
            oppgaveService
                .hentOppgaverForReferanse(forbehandlingId)
                .any { it.type == OppgaveType.ETTEROPPGJOER_OPPRETT_REVURDERING }

        if (oppgaveFinnesAllerede) {
            logger.info("Oppgave for svarfrist utløpt finnes allerede for forbehandlingId=$forbehandlingId")
            return
        }

        logger.info("Oppretter oppgave for svarfrist utløpt for forbehandlingId=$forbehandlingId")

        oppgaveService.opprettOppgave(
            referanse = forbehandlingId,
            sakId = etteroppgjoer.sakId,
            type = OppgaveType.ETTEROPPGJOER_OPPRETT_REVURDERING,
            merknad = "Svarfrist for etteroppgjør ${etteroppgjoer.inntektsaar} er utløpt",
            kilde = OppgaveKilde.HENDELSE,
            gjelderAar = etteroppgjoer.inntektsaar,
        )

        etteroppgjoerService.registrerHendelseForEtteroppgjoer(
            etteroppgjoer.sakId,
            etteroppgjoer.inntektsaar,
            EtteroppgjoerHendelser.SVARFRIST_UTLOEPT,
        )
    }
}
