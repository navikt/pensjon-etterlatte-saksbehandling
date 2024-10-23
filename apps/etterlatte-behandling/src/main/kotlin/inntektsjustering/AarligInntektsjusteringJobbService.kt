package no.nav.etterlatte.inntektsjustering

import no.nav.etterlatte.behandling.omregning.OmregningService
import no.nav.etterlatte.kafka.JsonMessage
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.inntektsjustering.AarligInntektsjusteringKjoering
import no.nav.etterlatte.libs.common.inntektsjustering.AarligInntektsjusteringRequest
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.rapidsandrivers.CORRELATION_ID_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.TEKNISK_TID_KEY
import no.nav.etterlatte.libs.common.sak.KjoeringRequest
import no.nav.etterlatte.libs.common.sak.KjoeringStatus
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.rapidsandrivers.OmregningData
import no.nav.etterlatte.rapidsandrivers.OmregningDataPacket
import no.nav.etterlatte.rapidsandrivers.OmregningHendelseType
import org.slf4j.LoggerFactory
import java.time.YearMonth

class AarligInntektsjusteringJobbService(
    private val omregningService: OmregningService,
    private val rapid: KafkaProdusent<String, String>,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun startAarligInntektsjustering(request: AarligInntektsjusteringRequest) {
        request.saker.forEach { sakId ->
            startEnkeltSak(request.kjoering, request.loependeFom, sakId)
        }
    }

    private fun startEnkeltSak(
        kjoering: String,
        loependeFom: YearMonth,
        sakId: SakId,
    ) {
        try {
            logger.info("Årlig inntektsjusteringsjobb $kjoering for $sakId")
            if (!skalBehandlingOmregnes(loependeFom)) {
                // TODO Legge til en begrunnelse
                omregningService.oppdaterKjoering(KjoeringRequest(kjoering, KjoeringStatus.FERDIGSTILT, sakId))
            } else if (kanIkkeKjoeresAutomatisk()) {
                // TODO Finnes det tilfeller av dette?
            } else {
                // TODO status KLAR_FOR_OMREGNING
                publiserKlarForOmregning(sakId, loependeFom)
            }
        } catch (e: Exception) {
            // TODO begrunnese!
            omregningService.oppdaterKjoering(KjoeringRequest(kjoering, KjoeringStatus.FEILA, sakId))
        }
    }

    private fun kanIkkeKjoeresAutomatisk(): Boolean {
        // TODO
        return false
    }

    private fun publiserKlarForOmregning(
        sakId: SakId,
        loependeFom: YearMonth,
    ) {
        val correlationId = getCorrelationId()
        rapid
            .publiser(
                "aarlig-inntektsjustering-$sakId",
                JsonMessage
                    .newMessage(
                        OmregningHendelseType.KLAR_FOR_OMREGNING.lagEventnameForType(),
                        mapOf(
                            CORRELATION_ID_KEY to correlationId,
                            TEKNISK_TID_KEY to Tidspunkt.now(),
                            OmregningDataPacket.KEY to
                                OmregningData(
                                    kjoering = AarligInntektsjusteringKjoering.getKjoering(),
                                    sakId = sakId,
                                    revurderingaarsak = Revurderingaarsak.INNTEKTSENDRING, // TODO egen årsak?
                                    fradato = loependeFom.atDay(1),
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

    private fun skalBehandlingOmregnes(loependeFom: YearMonth): Boolean {
        // TODO kall vedtakfor så om er innvilga / løpende

        // TODO kall berening for å se om det finnes inntekt fra 1.1

        return true
    }
}
