package no.nav.etterlatte.beregningkafka

import com.fasterxml.jackson.module.kotlin.treeToValue
import io.ktor.client.call.body
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.beregning.AvkortingDto
import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.setEventNameForHendelseType
import no.nav.etterlatte.rapidsandrivers.AVKORTING_KEY
import no.nav.etterlatte.rapidsandrivers.BEHANDLING_ID_KEY
import no.nav.etterlatte.rapidsandrivers.BEHANDLING_VI_OMREGNER_FRA_KEY
import no.nav.etterlatte.rapidsandrivers.BEREGNING_KEY
import no.nav.etterlatte.rapidsandrivers.HENDELSE_DATA_KEY
import no.nav.etterlatte.rapidsandrivers.ListenerMedLoggingOgFeilhaandtering
import no.nav.etterlatte.rapidsandrivers.ReguleringHendelseType
import no.nav.etterlatte.rapidsandrivers.SAK_TYPE
import no.nav.etterlatte.rapidsandrivers.behandlingId
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.toUUID
import org.slf4j.LoggerFactory

internal class OmregningHendelserRiver(
    rapidsConnection: RapidsConnection,
    private val beregningService: BeregningService,
    private val trygdetidService: TrygdetidService,
) : ListenerMedLoggingOgFeilhaandtering() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, ReguleringHendelseType.VILKAARSVURDERT) {
            validate { it.requireKey(BEHANDLING_ID_KEY) }
            validate { it.requireKey(SAK_TYPE) }
            validate { it.rejectKey(BEREGNING_KEY) }
            validate { it.requireKey(HENDELSE_DATA_KEY) }
            validate { it.requireKey(BEHANDLING_VI_OMREGNER_FRA_KEY) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        logger.info("Mottatt omregninghendelse")
        val behandlingId = packet.behandlingId
        val behandlingViOmregnerFra = packet[BEHANDLING_VI_OMREGNER_FRA_KEY].asText().toUUID()
        val sakType = objectMapper.treeToValue<SakType>(packet[SAK_TYPE])
        trygdetidService.kopierTrygdetidFraForrigeBehandling(behandlingId, behandlingViOmregnerFra)
        runBlocking {
            beregningService.opprettBeregningsgrunnlagFraForrigeBehandling(behandlingId, behandlingViOmregnerFra)
            val beregning = beregningService.beregn(behandlingId).body<BeregningDTO>()
            packet[BEREGNING_KEY] = beregning
            if (sakType == SakType.OMSTILLINGSSTOENAD) {
                val avkorting =
                    beregningService.regulerAvkorting(behandlingId, behandlingViOmregnerFra)
                        .body<AvkortingDto>()
                packet[AVKORTING_KEY] = avkorting
            }
            packet.setEventNameForHendelseType(ReguleringHendelseType.BEREGNA)
            context.publish(packet.toJson())
        }
        logger.info("Publiserte oppdatert omregningshendelse")
    }
}
