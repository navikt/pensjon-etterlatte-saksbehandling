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
            validate { it.demandValue("@vedtak_attestert", true) }
            validate { it.requireKey("@vedtak") }
            validate { it.rejectKey("@vedtak_oversatt") }
            validate { it.interestedIn("@correlation_id") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId()) {
            try {
                logger.info("Attestert vedtak mottatt")
                val vedtak: Vedtak = objectMapper.readValue(packet["@vedtak"].toJson(), Vedtak::class.java)
                val attestasjon: Attestasjon =
                    objectMapper.readValue(packet["@attestasjon"].toJson(), Attestasjon::class.java)

                logger.info("Oppretter utbetalingsoppdrag for sakId=${vedtak.sakId} med vedtakId=${vedtak.vedtakId}, attestert av: ${attestasjon.attestantId}")
                val oppdrag: Oppdrag = oppdragMapper.oppdragFraVedtak(vedtak, attestasjon)

                oppdragSender.sendOppdrag(oppdrag)

                //context.publish(packet.apply { this["@vedtak_oversatt"] = true }.toJson())
            } catch (e: Exception) {
                logger.error("En feil oppstod: ${e.message}", e)
            }
        }

    private fun JsonMessage.correlationId(): String? = get("@correlation_id").textValue()

}

