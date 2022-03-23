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
    val oppdragMapper: OppdragMapper,
    val oppdragSender: OppdragSender,
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
                logger.info("Mottok fattet vedtak - klargjører for oppdrag")
                val vedtak: Vedtak = objectMapper.readValue(packet["@vedtak"].toJson(), Vedtak::class.java)
                val oppdrag: Oppdrag = oppdragMapper.oppdragFraVedtak(vedtak)
                oppdragSender.sendOppdrag(oppdrag)

                logger.info("Oppdrag opprettet")

                //context.publish(packet.apply { this["@vedtak_oversatt"] = true }.toJson())
            } catch (e: Exception) {
                logger.error("En feil oppstod: ${e.message}", e)
            }
        }

    private fun JsonMessage.correlationId(): String? = get("@correlation_id").textValue()

}

