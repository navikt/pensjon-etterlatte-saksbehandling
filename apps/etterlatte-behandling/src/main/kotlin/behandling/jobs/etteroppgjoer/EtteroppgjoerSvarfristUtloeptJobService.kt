package no.nav.etterlatte.behandling.jobs.etteroppgjoer

import com.typesafe.config.Config
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.Context
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerService
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerStatus
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
    private val etteroppgjoerForbehandlingService: EtteroppgjoerForbehandlingService,
    private val etteroppgjoerService: EtteroppgjoerService,
    private val oppgaveService: OppgaveService,
    private val featureToggleService: FeatureToggleService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private val svarfrist = config.getString("etteroppgjoer.svarfristutloept.jobb.interval")

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

        val forbehandlinger =
            etteroppgjoerForbehandlingService
                .hentForbehandlingMedSvarfristUtloept(inntektsaar, svarfrist)
                .orEmpty()

        val skalHaOppgaveSvarfristUtloept =
            forbehandlinger.filter { forbehandling ->
                val etteroppgjoer =
                    etteroppgjoerService
                        .hentEtteroppgjoerForInntektsaar(forbehandling.sak.id, inntektsaar)
                        ?: throw InternfeilException(
                            "ForbehandlingId=${forbehandling.id} forventet etteroppgjør for sakId=${forbehandling.sak.id} og inntektsaar=$inntektsaar men var NULL",
                        )
                etteroppgjoer.status == EtteroppgjoerStatus.FERDIGSTILT_FORBEHANDLING
            }

        val antallOppgaverOpprettet =
            skalHaOppgaveSvarfristUtloept.count { forbehandling ->
                val finnesAllerede =
                    oppgaveService
                        .hentOppgaverForSakAvType(
                            forbehandling.sak.id,
                            listOf(OppgaveType.ETTEROPPGJOER_SVARFRIST_UTLOEPT),
                        ).any { it.opprettet.toLocalDate().year == inntektsaar }

                if (finnesAllerede) {
                    logger.info("Oppgave for svarfrist utløpt finnes allerede for forbehandlingId=${forbehandling.id}")
                    false
                } else {
                    logger.info("Oppretter oppgave for svarfrist utløpt for forbehandlingId=${forbehandling.id}")
                    oppgaveService.opprettOppgave(
                        referanse = forbehandling.id.toString(),
                        sakId = forbehandling.sak.id,
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
