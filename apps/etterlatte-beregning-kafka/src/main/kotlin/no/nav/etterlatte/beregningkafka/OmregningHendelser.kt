package no.nav.etterlatte.beregningkafka

import io.ktor.client.call.body
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.rapidsandrivers.EventNames.BEREGN
import no.nav.etterlatte.rapidsandrivers.EventNames.OPPRETT_VEDTAK
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import rapidsandrivers.BEREGNING_KEY
import rapidsandrivers.HENDELSE_DATA_KEY
import rapidsandrivers.OMREGNING_ID_KEY
import rapidsandrivers.omregningId
import rapidsandrivers.withFeilhaandtering

internal class OmregningHendelser(
    rapidsConnection: RapidsConnection,
    private val beregningService: BeregningService
) :
    River.PacketListener {

    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        logger.info("initierer rapid for omregninghendelser")
        River(rapidsConnection).apply {
            eventName(BEREGN)

            correlationId()
            validate { it.requireKey(OMREGNING_ID_KEY) }
            validate { it.rejectKey(BEREGNING_KEY) }
            validate { it.requireKey(HENDELSE_DATA_KEY) }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        withLogContext(packet.correlationId) {
            withFeilhaandtering(packet, context, BEREGN) {
                logger.info("Mottatt omregninghendelse")
                val omregningsId = packet.omregningId
                runBlocking {
                    val beregning = beregningService.opprettOmregning(omregningsId).body<BeregningDTO>()
                    packet[BEREGNING_KEY] = beregning
                    packet[EVENT_NAME_KEY] = OPPRETT_VEDTAK
                    context.publish(packet.toJson())
                }
                logger.info("Publiserte oppdatert omregningshendelse")
            }
        }
    }
}