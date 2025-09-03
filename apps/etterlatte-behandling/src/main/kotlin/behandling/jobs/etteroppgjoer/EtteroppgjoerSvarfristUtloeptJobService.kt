package no.nav.etterlatte.behandling.jobs.etteroppgjoer

import com.typesafe.config.Config
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.Context
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerService
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerStatus
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerSvarfrist
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerToggles
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandlingService
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.oppgave.OppgaveService
import org.slf4j.LoggerFactory
import java.time.YearMonth

@OptIn(DelicateCoroutinesApi::class)
class EtteroppgjoerSvarfristUtloeptJobService(
    private val config: Config,
    private val etteroppgjoerService: EtteroppgjoerService,
    private val oppgaveService: OppgaveService,
    private val featureToggleService: FeatureToggleService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private val svarfrist = EtteroppgjoerSvarfrist.valueOf(config.getString("etteroppgjoer.svarfrist"))

    fun startKjoering(jobContext: Context) {
        Kontekst.set(jobContext)
        if (featureToggleService.isEnabled(EtteroppgjoerToggles.ETTEROPPGJOER_SVARFRISTUTLOEPT_JOBB, false)) {
            logger.info("Starter periodiske jobber for opprette oppgave svarfrist utløpt etteroppgjoer")
            runBlocking {
                opprettNyOppgaveSvarfristUtloept()
            }
        } else {
            logger.info("Periodisk jobber for opprette oppgave svarfrist utløpt er deaktivert")
        }
    }

    private fun opprettNyOppgaveSvarfristUtloept() {
        // TODO: trekke ut til felles for alle etteroppgjør jobber
        val inntektsaar = YearMonth.now().year - 1

        val relevanteEtteroppgjoer = etteroppgjoerService.hentEtteroppgjoerMedSvarfristUtloept(inntektsaar, svarfrist)

        val antallOppgaverOpprettet =
            relevanteEtteroppgjoer?.count { etteroppgjoer ->

                val oppgaveFinnesAllerede =
                    oppgaveService
                        .hentOppgaverForReferanse(
                            etteroppgjoer.sisteFerdigstilteForbehandling.toString(),
                        ).any { it.type == OppgaveType.ETTEROPPGJOER_SVARFRIST_UTLOEPT }

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
                        type = OppgaveType.ETTEROPPGJOER_SVARFRIST_UTLOEPT,
                        merknad = "Svarfrist for etteroppgjør $inntektsaar er utløpt",
                        kilde = OppgaveKilde.HENDELSE,
                    )
                    true
                }
            }

        logger.info("Opprettet $antallOppgaverOpprettet oppgaver for etteroppgjør med svarfrist utløpt for $inntektsaar")
    }
}
