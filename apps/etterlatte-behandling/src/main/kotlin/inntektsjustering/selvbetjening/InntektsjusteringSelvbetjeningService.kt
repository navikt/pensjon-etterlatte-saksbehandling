package no.nav.etterlatte.inntektsjustering.selvbetjening

import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.kafka.JsonMessage
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.inntektsjustering.InntektsjusteringRequest
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.rapidsandrivers.CORRELATION_ID_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.TEKNISK_TID_KEY
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.rapidsandrivers.OmregningData
import no.nav.etterlatte.rapidsandrivers.OmregningDataPacket
import no.nav.etterlatte.rapidsandrivers.OmregningHendelseType
import no.nav.etterlatte.rapidsandrivers.OmregningInntektsjustering
import org.slf4j.LoggerFactory
import java.time.YearMonth

class InntektsjusteringSelvbetjeningService(
    private val oppgaveService: OppgaveService,
    private val rapid: KafkaProdusent<String, String>,
    private val featureToggleService: FeatureToggleService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun behandleInntektsjustering(request: InntektsjusteringRequest) {
        logger.info("Starter behandling av innmeldt inntektsjustering for sak ${request.sak.sakId}")

        if (skalGjoeresAutomatisk()) {
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
        publiserKlarForOmregning(
            sakId,
            InntektsjusteringRequest.utledLoependeFom(),
            InntektsjusteringRequest.utledKjoering(request.inntektsjusteringId),
            OmregningInntektsjustering(
                inntekt = request.inntekt,
                inntektUtland = request.inntektUtland,
            ),
        )
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

    private fun publiserKlarForOmregning(
        sakId: SakId,
        loependeFom: YearMonth,
        kjoering: String,
        inntektsjustering: OmregningInntektsjustering,
    ) {
        val correlationId = getCorrelationId()
        rapid
            .publiser(
                "inntektsjustering-$sakId",
                JsonMessage
                    .newMessage(
                        OmregningHendelseType.KLAR_FOR_OMREGNING.lagEventnameForType(),
                        mapOf(
                            CORRELATION_ID_KEY to correlationId,
                            TEKNISK_TID_KEY to Tidspunkt.now(),
                            OmregningDataPacket.KEY to
                                OmregningData(
                                    kjoering = kjoering,
                                    sakId = sakId,
                                    revurderingaarsak = Revurderingaarsak.INNTEKTSENDRING,
                                    fradato = loependeFom.atDay(1),
                                    inntektsjustering = inntektsjustering,
                                ).toPacket(),
                        ),
                    ).toJson(),
            ).also { (partition, offset) ->
                logger.info(
                    "Publiserte klar for omregningshendelse for $sakId på partition " +
                        "$partition, offset $offset, correlationid: $correlationId",
                )
            }
    }

    private fun skalGjoeresAutomatisk(): Boolean {
        val featureToggle =
            featureToggleService.isEnabled(
                InntektsjusterinFeatureToggle.AUTOMATISK_BEHANDLE,
                false,
            )

        // TODO: sjekke om riktig tilstand for automatisk behandling
        return featureToggle
    }

    enum class InntektsjusterinFeatureToggle(
        private val key: String,
    ) : FeatureToggle {
        AUTOMATISK_BEHANDLE("inntektsjustering-automatisk-behandle"),
        ;

        override fun key() = key
    }
}
