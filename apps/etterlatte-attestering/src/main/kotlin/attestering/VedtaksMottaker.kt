package no.nav.etterlatte.attestering

import no.nav.etterlatte.domain.Attestasjon
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
            validate { it.rejectKey("@attestasjon") }
            validate { it.interestedIn("@correlation_id") }
        }.register(this)
    }


    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId()) {
            try {
                logger.info("Fattet vedtak mottatt")
                val vedtak: Vedtak = objectMapper.readValue(packet["@vedtak"].toJson(), Vedtak::class.java)
                packet["@attestasjon"] = attester()
                context.publish(packet.toJson())
                logger.info("Vedtak attestert og lagt tilbake til RR")
            } catch (e: Exception) {
                logger.error("En feil oppstod: ${e.message}", e)
            }
        }

    private fun attester() = Attestasjon("Z123456")

    private fun JsonMessage.correlationId(): String? = get("@correlation_id").textValue()

}

