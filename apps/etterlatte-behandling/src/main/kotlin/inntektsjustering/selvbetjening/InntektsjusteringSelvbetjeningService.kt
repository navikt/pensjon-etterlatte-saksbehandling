package no.nav.etterlatte.inntektsjustering.selvbetjening

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.inTransaction
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
import no.nav.etterlatte.libs.ktor.token.HardkodaSystembruker
import no.nav.etterlatte.omregning.OmregningData
import no.nav.etterlatte.omregning.OmregningDataPacket
import no.nav.etterlatte.omregning.OmregningHendelseType
import no.nav.etterlatte.omregning.OmregningInntektsjustering
import no.nav.etterlatte.oppgave.OppgaveService
import org.slf4j.LoggerFactory
import java.time.YearMonth

class InntektsjusteringSelvbetjeningService(
    private val oppgaveService: OppgaveService,
    private val behandlingService: BehandlingService,
    private val vedtakKlient: VedtakKlient,
    private val rapid: KafkaProdusent<String, String>,
    private val featureToggleService: FeatureToggleService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun behandleInntektsjustering(request: InntektsjusteringRequest) {
        logger.info("Starter behandling av innmeldt inntektsjustering for sak ${request.sak.sakId}")

        if (skalGjoeresAutomatisk(request.sak)) {
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

    private fun startManuellBehandling(request: InntektsjusteringRequest) =
        inTransaction {
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

    private fun skalGjoeresAutomatisk(sakId: SakId): Boolean {
        if (!featureToggleService.isEnabled(
                InntektsjusterinFeatureToggle.AUTOMATISK_BEHANDLE,
                false,
            )
        ) {
            return false
        }

        val aapneBehandlinger = runBlocking { behandlingService.hentAapneBehandlingerForSak(sakId) }
        if (aapneBehandlinger.isNotEmpty()) return false

        val vedtak =
            runBlocking {
                vedtakKlient.sakHarLopendeVedtakPaaDato(
                    sakId,
                    InntektsjusteringRequest.utledLoependeFom().atDay(1),
                    HardkodaSystembruker.omregning,
                )
            }

        if (!vedtak.erLoepende || vedtak.underSamordning) return false

        // TODO: flere sjekker?

        return true
    }

    enum class InntektsjusterinFeatureToggle(
        private val key: String,
    ) : FeatureToggle {
        AUTOMATISK_BEHANDLE("inntektsjustering-automatisk-behandle"),
        ;

        override fun key() = key
    }
}
