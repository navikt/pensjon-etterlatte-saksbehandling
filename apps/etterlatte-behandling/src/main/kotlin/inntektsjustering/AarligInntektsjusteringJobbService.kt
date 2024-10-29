package no.nav.etterlatte.inntektsjustering

import no.nav.etterlatte.behandling.klienter.BeregningKlient
import no.nav.etterlatte.behandling.klienter.VedtakKlient
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
import no.nav.etterlatte.libs.ktor.token.HardkodaSystembruker
import no.nav.etterlatte.rapidsandrivers.OmregningData
import no.nav.etterlatte.rapidsandrivers.OmregningDataPacket
import no.nav.etterlatte.rapidsandrivers.OmregningHendelseType
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.YearMonth

class AarligInntektsjusteringJobbService(
    private val omregningService: OmregningService,
    private val vedtakKlient: VedtakKlient,
    private val beregningKlient: BeregningKlient,
    private val rapid: KafkaProdusent<String, String>,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun startAarligInntektsjustering(request: AarligInntektsjusteringRequest) {
        request.saker.forEach { sakId ->
            startEnkeltSak(request.kjoering, request.loependeFom, sakId)
        }
    }

    private suspend fun startEnkeltSak(
        kjoering: String,
        loependeFom: YearMonth,
        sakId: SakId,
    ) {
        try {
            logger.info("Årlig inntektsjusteringsjobb $kjoering for $sakId")
            if (!skalBehandlingOmregnes(sakId, loependeFom)) {
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

    // Skal inntektjusteres hvis: 1) er løpende fom dato, 2) ikke har oppgitt inntekt fra 1.1 neste inntektsår
    private suspend fun skalBehandlingOmregnes(
        sakId: SakId,
        loependeFom: YearMonth,
    ): Boolean {
        val fomDato = LocalDate.of(loependeFom.year, loependeFom.month.value, 1)
        val harLoependeYtelse = vedtakKlient.sakHarLopendeVedtakPaaDato(sakId, fomDato, HardkodaSystembruker.omregning)
        if (!harLoependeYtelse.erLoepende) return false

        if (harLoependeYtelse.behandlingId == null) {
            // TODO hva gjør vi her?
        }

        val harOppgittInntektNesteAar = beregningKlient.harInntektNesteAar(harLoependeYtelse.behandlingId!!, HardkodaSystembruker.omregning)
        return !harOppgittInntektNesteAar
    }
}
