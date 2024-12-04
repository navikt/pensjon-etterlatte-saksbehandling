package no.nav.etterlatte.inntektsjustering.selvbetjening

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.klienter.BeregningKlient
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.kafka.JsonMessage
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.beregning.InntektsjusteringAvkortingInfoResponse
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.rapidsandrivers.CORRELATION_ID_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.TEKNISK_TID_KEY
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.inntektsjustering.MottattInntektsjustering
import no.nav.etterlatte.libs.inntektsjustering.MottattInntektsjusteringHendelseType
import no.nav.etterlatte.libs.ktor.token.HardkodaSystembruker
import no.nav.etterlatte.omregning.OmregningData
import no.nav.etterlatte.omregning.OmregningDataPacket
import no.nav.etterlatte.omregning.OmregningHendelseType
import no.nav.etterlatte.oppgave.OppgaveService
import org.slf4j.LoggerFactory
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.UUID

class InntektsjusteringSelvbetjeningService(
    private val oppgaveService: OppgaveService,
    private val behandlingService: BehandlingService,
    private val beregningKlient: BeregningKlient,
    private val vedtakKlient: VedtakKlient,
    private val rapid: KafkaProdusent<String, String>,
    private val featureToggleService: FeatureToggleService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun behandleInntektsjustering(mottattInntektsjustering: MottattInntektsjustering) {
        logger.info("Starter behandling av innmeldt inntektsjustering for sak ${mottattInntektsjustering.sak.sakId}")

        if (skalGjoeresAutomatisk(mottattInntektsjustering.sak)) {
            startAutomatiskBehandling(mottattInntektsjustering)
        } else {
            startManuellBehandling(mottattInntektsjustering)
        }
        mottattInntektsjsuteringFullfoert(mottattInntektsjustering.sak, mottattInntektsjustering.inntektsjusteringId)
    }

    private fun startAutomatiskBehandling(mottattInntektsjustering: MottattInntektsjustering) {
        logger.info("Behandles automatisk: starter omregning for sak ${mottattInntektsjustering.sak.sakId}")
        val correlationId = getCorrelationId()
        rapid
            .publiser(
                "inntektsjustering-${mottattInntektsjustering.sak}",
                JsonMessage
                    .newMessage(
                        OmregningHendelseType.KLAR_FOR_OMREGNING.lagEventnameForType(),
                        mapOf(
                            CORRELATION_ID_KEY to correlationId,
                            TEKNISK_TID_KEY to Tidspunkt.now(),
                            OmregningDataPacket.KEY to
                                OmregningData(
                                    kjoering = MottattInntektsjustering.utledKjoering(mottattInntektsjustering.inntektsjusteringId),
                                    sakId = mottattInntektsjustering.sak,
                                    revurderingaarsak = Revurderingaarsak.INNTEKTSENDRING,
                                    fradato = MottattInntektsjustering.utledLoependeFom().atDay(1),
                                    inntektsjustering = mottattInntektsjustering,
                                ).toPacket(),
                        ),
                    ).toJson(),
            ).also { (partition, offset) ->
                logger.info(
                    "Publiserte klar for omregningshendelse for ${mottattInntektsjustering.sak} på partition " +
                        "$partition, offset $offset, correlationid: $correlationId",
                )
            }
    }

    private fun startManuellBehandling(mottattInntektsjustering: MottattInntektsjustering) =
        inTransaction {
            logger.info("Behandles manuelt: oppretter oppgave for mottatt inntektsjustering for sak ${mottattInntektsjustering.sak.sakId}")
            oppgaveService.opprettOppgave(
                sakId = SakId(mottattInntektsjustering.sak.sakId),
                kilde = OppgaveKilde.BRUKERDIALOG,
                type = OppgaveType.INNTEKTSOPPLYSNING,
                merknad = "Ny inntektsopplysning",
                frist = Tidspunkt.now().plus(7L, ChronoUnit.DAYS),
                referanse = mottattInntektsjustering.journalpostId,
            )
        }

    private suspend fun skalGjoeresAutomatisk(sakId: SakId): Boolean {
        if (!featureToggleService.isEnabled(
                InntektsjusterinFeatureToggle.AUTOMATISK_BEHANDLE,
                false,
            )
        ) {
            return false
        }
        val loependeFom = MottattInntektsjustering.utledLoependeFom().atDay(1)

        // har åpne behandlinger
        val aapneBehandlinger = inTransaction { behandlingService.hentAapneBehandlingerForSak(sakId) }
        if (aapneBehandlinger.isNotEmpty()) return false

        // ikke loepende || er under samordning
        val vedtak =
            vedtakKlient.sakHarLopendeVedtakPaaDato(
                sakId,
                loependeFom,
                HardkodaSystembruker.omregning,
            )
        if (!vedtak.erLoepende || vedtak.underSamordning) return false

        // har sanksjon
        val avkortingSjekk = hentAvkortingSjekk(sakId, YearMonth.from(loependeFom), vedtak.behandlingId!!)
        if (avkortingSjekk.harSanksjon) return false

        return true
    }

    private fun mottattInntektsjsuteringFullfoert(
        sakId: SakId,
        inntektsjusteirngId: UUID,
    ) {
        logger.info("Mottak av inntektsjustering fullført sender melding til selvbetjening sak=$sakId")
        val correlationId = getCorrelationId()
        val hendelsetype = MottattInntektsjusteringHendelseType.MOTTAK_FULLFOERT.lagEventnameForType()
        rapid
            .publiser(
                "mottak-inntektsjustering-fullfoert-$sakId",
                JsonMessage
                    .newMessage(
                        hendelsetype,
                        mapOf(
                            CORRELATION_ID_KEY to correlationId,
                            TEKNISK_TID_KEY to Tidspunkt.now(),
                            "inntektsjustering_id" to inntektsjusteirngId,
                        ),
                    ).toJson(),
            ).also { (partition, offset) ->
                logger.info(
                    "Publiserte $hendelsetype for $sakId på partition " +
                        "$partition, offset $offset, correlationid: $correlationId",
                )
            }
    }

    enum class InntektsjusterinFeatureToggle(
        private val key: String,
    ) : FeatureToggle {
        AUTOMATISK_BEHANDLE("inntektsjustering-automatisk-behandle"),
        ;

        override fun key() = key
    }

    private fun hentAvkortingSjekk(
        sakId: SakId,
        loependeFom: YearMonth,
        forrigeBehandlingId: UUID,
    ): InntektsjusteringAvkortingInfoResponse =
        runBlocking {
            beregningKlient.inntektsjusteringAvkortingInfoSjekk(
                sakId,
                loependeFom.year,
                forrigeBehandlingId,
                HardkodaSystembruker.omregning,
            )
        }
}
