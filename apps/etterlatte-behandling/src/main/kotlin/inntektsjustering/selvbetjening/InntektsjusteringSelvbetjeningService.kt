package no.nav.etterlatte.inntektsjustering.selvbetjening

import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.GrunnlagService
import no.nav.etterlatte.behandling.klienter.BeregningKlient
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.behandling.omregning.OmregningService
import no.nav.etterlatte.behandling.revurdering.RevurderingService
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.inntektsjustering.InntektsjusterinFeatureToggle
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.libs.common.inntektsjustering.InntektsjusteringRequest
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.sak.SakService
import org.slf4j.LoggerFactory

class InntektsjusteringSelvbetjeningService(
    private val omregningService: OmregningService,
    private val sakService: SakService,
    private val behandlingService: BehandlingService,
    private val revurderingService: RevurderingService,
    private val grunnlagService: GrunnlagService,
    private val vedtakKlient: VedtakKlient,
    private val beregningKlient: BeregningKlient,
    private val pdlTjenesterKlient: PdlTjenesterKlient,
    private val oppgaveService: OppgaveService,
    private val rapid: KafkaProdusent<String, String>,
    private val featureToggleService: FeatureToggleService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun behandleInntektsjustering(request: InntektsjusteringRequest) {
        logger.info("Starter behandling av innmeldt inntektsjustering for sak ${request.sak.sakId}")
        val skalGjoeresAutomatisk =
            featureToggleService.isEnabled(
                InntektsjusterinFeatureToggle.AUTOMATISK_BEHANDLE,
                false,
            )

        if (skalGjoeresAutomatisk) {
            startAutomatiskBehandling(
                request,
                SakId(request.sak.sakId),
            )
        } else {
            startManuellBehandling(request)
        }
    }

    private fun startAutomatiskBehandling(
        request: InntektsjusteringRequest,
        sakId: SakId,
    ) {
        logger.info("Behandles automatisk: starter omregning for sak ${request.sak.sakId}")
        // TODO: behandle automatisk
    }

    private fun startManuellBehandling(request: InntektsjusteringRequest) {
        logger.info("Behandles manuelt: oppretter oppgave for mottatt inntektsjustering for sak ${request.sak.sakId}")
        oppgaveService.opprettOppgave(
            sakId = SakId(request.sak.sakId),
            kilde = OppgaveKilde.BRUKERDIALOG,
            type = OppgaveType.MOTTATT_INNTEKTSJUSTERING,
            merknad = "Mottatt inntektsjustering",
            referanse = request.journalpostId,
        )
    }
}
