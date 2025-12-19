package no.nav.etterlatte.behandling.jobs.etteroppgjoer

import kotlinx.coroutines.DelicateCoroutinesApi
import no.nav.etterlatte.Context
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.etteroppgjoer.ETTEROPPGJOER_AAR
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerService
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerSvarfrist
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerToggles
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.inTransaction
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
        if (featureToggleService.isEnabled(EtteroppgjoerToggles.ETTEROPPGJOER_SVARFRISTUTLOEPT_JOBB, false)) {
            logger.info("Starter periodiske jobber for opprette oppgave svarfrist utløpt etteroppgjoer")
            inTransaction {
                opprettNyOppgaveSvarfristUtloept()
            }
        } else {
            logger.info("Periodisk jobber for opprette oppgave svarfrist utløpt er deaktivert")
        }
    }

    private fun opprettNyOppgaveSvarfristUtloept() {
        val etteroppgjoersAar = ETTEROPPGJOER_AAR
        val relevanteEtteroppgjoer = etteroppgjoerService.hentEtteroppgjoerMedSvarfristUtloept(etteroppgjoersAar, svarfrist)

        val antallOppgaverOpprettet =
            relevanteEtteroppgjoer?.count { etteroppgjoer ->

                val oppgaveFinnesAllerede =
                    oppgaveService
                        .hentOppgaverForReferanse(
                            etteroppgjoer.sisteFerdigstilteForbehandling.toString(),
                        ).any { it.type == OppgaveType.ETTEROPPGJOER_OPPRETT_REVURDERING }

                if (oppgaveFinnesAllerede) {
                    logger.info(
                        "Oppgave for svarfrist utløpt finnes allerede for forbehandlingId=${etteroppgjoer.sisteFerdigstilteForbehandling}",
                    )
                    false
                } else {
                    logger.info(
                        "Oppretter oppgave for svarfrist utløpt for forbehandlingId=${etteroppgjoer.sisteFerdigstilteForbehandling}",
                    )
                    oppgaveService.opprettOppgave(
                        referanse = etteroppgjoer.sisteFerdigstilteForbehandling.toString(),
                        sakId = etteroppgjoer.sakId,
                        type = OppgaveType.ETTEROPPGJOER_OPPRETT_REVURDERING,
                        merknad = "Svarfrist for etteroppgjør $etteroppgjoersAar er utløpt",
                        kilde = OppgaveKilde.HENDELSE,
                    )
                    true
                }
            }

        logger.info("Opprettet $antallOppgaverOpprettet oppgaver for etteroppgjør med svarfrist utløpt for $etteroppgjoersAar")
    }
}
