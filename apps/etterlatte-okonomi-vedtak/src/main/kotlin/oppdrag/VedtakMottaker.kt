package no.nav.etterlatte.oppdrag


import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.domain.Attestasjon
import no.nav.etterlatte.domain.Vedtak
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory


internal class VedtakMottaker(
    rapidsConnection: RapidsConnection,
    private val oppdragService: OppdragService,
) : River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "vedtak_fattet") }
            validate { it.requireKey("@vedtak") }
            validate { it.requireKey("@attestasjon") }
            validate { it.rejectKey("@vedtak_oversatt") }
            validate { it.interestedIn("@correlation_id") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId()) {
            try {
                logger.info("Attestert vedtak mottatt")
                val vedtak: Vedtak = objectMapper.readValue(packet["@vedtak"].toJson())
                val attestasjon: Attestasjon = objectMapper.readValue(packet["@attestasjon"].toJson())
                oppdragService.opprettOgSendOppdrag(vedtak, attestasjon)
            } catch (e: Exception) {
                logger.error("En feil oppstod: ${e.message}", e)
            }
        }

    private fun JsonMessage.correlationId(): String? = get("@correlation_id").textValue()

    companion object {
        private val logger = LoggerFactory.getLogger(VedtakMottaker::class.java)
    }
}

