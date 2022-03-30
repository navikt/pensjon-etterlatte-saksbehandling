package no.nav.etterlatte.attestering

import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory


internal class VedtaksMottaker(
    rapidsConnection: RapidsConnection,
) : River.PacketListener {

    private val logger = LoggerFactory.getLogger(VedtaksMottaker::class.java)

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "vedtak_fattet") }
            validate { it.requireKey("@vedtak") }
            validate { it.rejectKey("@vedtak_attestert") }
            validate { it.interestedIn("@correlation_id") }
        }.register(this)
    }


    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId()) {
            try {
                logger.info("Fattet vedtak mottatt")
                val vedtak: Vedtak = objectMapper.readValue(packet["@vedtak"].toJson(), Vedtak::class.java)
                context.publish(packet.toAttestertEvent().toJson())
                logger.info("Vedtak attestert")
            } catch (e: Exception) {
                logger.error("En feil oppstod: ${e.message}", e)
            }
        }

    private fun attester() = Attestasjon("Z123456")

    private fun JsonMessage.toAttestertEvent() = apply {
        this["@vedtak_attestert"] = true
        this["@attestasjon"] = attester().toJson()
        this["@correlation_id"] = getCorrelationId()
    }

    private fun JsonMessage.correlationId(): String? = get("@correlation_id").textValue()

}

