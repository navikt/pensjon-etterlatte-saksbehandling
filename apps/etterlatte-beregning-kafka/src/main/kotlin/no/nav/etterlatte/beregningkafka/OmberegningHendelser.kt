package no.nav.etterlatte.beregningkafka

import com.fasterxml.jackson.module.kotlin.treeToValue
import io.ktor.client.call.body
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.libs.common.rapidsandrivers.eventNameKey
import no.nav.etterlatte.rapidsandrivers.EventNames.OMBEREGNINGSHENDELSE
import no.nav.etterlatte.rapidsandrivers.EventNames.OPPRETT_VEDTAK
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import rapidsandrivers.beregningKey
import rapidsandrivers.hendelseDataKey
import rapidsandrivers.omberegningIdKey
import java.util.*

internal class OmberegningHendelser(
    rapidsConnection: RapidsConnection,
    private val beregningService: BeregningService
) :
    River.PacketListener {

    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        logger.info("initierer rapid for omberegninghendelser")
        River(rapidsConnection).apply {
            eventName(OMBEREGNINGSHENDELSE)

            correlationId()
            validate { it.requireKey(omberegningIdKey) }
            validate { it.rejectKey(beregningKey) }
            validate { it.requireKey(hendelseDataKey) }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        withLogContext(packet.correlationId) {
            logger.info("Mottatt omberegninghendelse")
            try {
                val omberegningsId: UUID = objectMapper.treeToValue(packet[omberegningIdKey])
                runBlocking {
                    val beregning = beregningService.opprettOmberegning(omberegningsId).body<BeregningDTO>()
                    packet[beregningKey] = beregning
                    packet[eventNameKey] = OPPRETT_VEDTAK
                    context.publish(packet.toJson())
                }
                logger.info("Publiserte oppdatert omberegningshendelse")
            } catch (e: Exception) {
                logger.error("Feil oppstod under lesing / sending av hendelse til beregning ", e)
            }
        }
    }
}