package no.nav.etterlatte.beregningkafka

import com.fasterxml.jackson.module.kotlin.treeToValue
import io.ktor.client.call.body
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.beregning.AvkortingDto
import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.rapidsandrivers.EventNames.BEREGN
import no.nav.etterlatte.rapidsandrivers.EventNames.OPPRETT_VEDTAK
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.toUUID
import rapidsandrivers.AVKORTING_KEY
import rapidsandrivers.BEHANDLING_ID_KEY
import rapidsandrivers.BEHANDLING_VI_OMREGNER_FRA_KEY
import rapidsandrivers.BEREGNING_KEY
import rapidsandrivers.HENDELSE_DATA_KEY
import rapidsandrivers.SAK_TYPE
import rapidsandrivers.behandlingId
import rapidsandrivers.migrering.RiverMedLoggingOgFeilhaandtering

internal class OmregningHendelser(
    rapidsConnection: RapidsConnection,
    private val beregningService: BeregningService,
    private val trygdetidService: TrygdetidService
) :
    RiverMedLoggingOgFeilhaandtering(rapidsConnection, BEREGN) {

    init {
        initialiser {
            eventName(hendelsestype)

            validate { it.requireKey(BEHANDLING_ID_KEY) }
            validate { it.requireKey(SAK_TYPE) }
            validate { it.rejectKey(BEREGNING_KEY) }
            validate { it.requireKey(HENDELSE_DATA_KEY) }
            validate { it.requireKey(BEHANDLING_VI_OMREGNER_FRA_KEY) }
        }
    }

    override fun haandterPakke(packet: JsonMessage, context: MessageContext) {
        logger.info("Mottatt omregninghendelse")
        val behandlingId = packet.behandlingId
        val behandlingViOmregnerFra = packet[BEHANDLING_VI_OMREGNER_FRA_KEY].asText().toUUID()
        val sakType = objectMapper.treeToValue<SakType>(packet[SAK_TYPE])
        runBlocking {
            if (sakType == SakType.BARNEPENSJON) {
                beregningService.opprettBeregningsGrunnlag(behandlingId, behandlingViOmregnerFra)
                val beregning = beregningService.beregn(behandlingId).body<BeregningDTO>()
                packet[BEREGNING_KEY] = beregning
            } else {
                trygdetidService.kopierTrygdetidFraForrigeBehandling(behandlingId, behandlingViOmregnerFra)
                val beregning = beregningService.beregn(behandlingId).body<BeregningDTO>()
                packet[BEREGNING_KEY] = beregning
                val avkorting = beregningService.regulerAvkorting(behandlingId, behandlingViOmregnerFra)
                    .body<AvkortingDto>()
                packet[AVKORTING_KEY] = avkorting
            }
            packet[EVENT_NAME_KEY] = OPPRETT_VEDTAK
            context.publish(packet.toJson())
        }
        logger.info("Publiserte oppdatert omregningshendelse")
    }
}