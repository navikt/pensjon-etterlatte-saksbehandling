package no.nav.etterlatte.vedtaksoversetter


import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import org.slf4j.LoggerFactory

internal class Vedtaksoversetter(
    rapidsConnection: RapidsConnection,
    val oppdragMapper: OppdragMapper
) : River.PacketListener {

    private val logger = LoggerFactory.getLogger(Vedtaksoversetter::class.java)

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "vedtak_fattet") }
            validate { it.requireKey("@vedtak") }
            validate { it.rejectKey("@vedtak_oversatt") }
            validate { it.interestedIn("@correlation_id") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId()) {
            try {
                // TODO finne relevante felter i vedtak
                val vedtak: Vedtak = objectMapper.readValue(packet["@vedtak"].toJson(), Vedtak::class.java)

                // TODO finne ut hvordan oppdrag skal bygges opp
                val oppdrag: Oppdrag = oppdragMapper.oppdragFraVedtak(vedtak)

                // TODO send oppdrag til MQ-tjeneste - krever tilgang til tjeneste som ligger onprem
                sendOppdrag(oppdrag)

                logger.info("Oppdrag opprettet")
                context.publish(packet.apply { this["@vedtak_oversatt"] = true }.toJson())
            } catch (e: Exception) {
                logger.error("Uhåndtert feilsituasjon: ${e.message}", e)
            }
        }

    private fun sendOppdrag(oppdrag: Oppdrag) {
        val xml = oppdrag.toXml()
        // send
    }

    private fun JsonMessage.correlationId(): String? = get("@correlation_id").textValue()

}

