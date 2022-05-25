package no.nav.etterlatte.utbetaling.iverksetting


import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.domene.vedtak.VedtakType
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Utbetaling
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingService
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Utbetalingsvedtak
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory


class VedtakMottaker(
    private val rapidsConnection: RapidsConnection,
    private val utbetalingService: UtbetalingService,
) : River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "vedtak_fattet") }
            validate { it.requireKey("@vedtak") }
            validate { it.requireAny("@vedtak.type", listOf(VedtakType.INNVILGELSE.name, VedtakType.OPPHOER.name)) }
            validate { it.interestedIn("@correlation_id") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId()) {
            try {
                val vedtak: Utbetalingsvedtak = objectMapper.readValue(packet["@vedtak"].toJson())
                logger.info("Attestert vedtak med vedtakId=${vedtak.vedtakId} mottatt")
                try {

                    if (utbetalingService.utbetalingEksisterer(vedtak)) {
                        logger.info("Vedtak eksisterer fra før. Sendes ikke videre til oppdrag")
                        rapidsConnection.publish(utbetalingEksisterer(vedtak))
                    } else {
                        val utbetaling = utbetalingService.iverksettUtbetaling(vedtak)
                        rapidsConnection.publish("key", utbetalingEvent(utbetaling))
                    }
                } catch (e: Exception) {
                    logger.error("En feil oppstod: ${e.message}", e)
                    rapidsConnection.publish(utbetalingFeilet(vedtak))
                    throw e
                }
            } catch (e: Exception) {
                if (e is InvalidFormatException || e is MissingKotlinParameterException) {
                    logger.error("Kunne ikke deresialisere vedtak: ", packet["@vedtak"].toJson())
                    rapidsConnection.publish(deserialiseringFeilet(packet["@vedtak"]))
                } else throw e
            }
        }


    private fun utbetalingEvent(utbetaling: Utbetaling) = mapOf(
        "@event_name" to "utbetaling_oppdatert",
        "@vedtakId" to utbetaling.vedtakId.value,
        "@status" to utbetaling.status.name
    ).toJson()

    private fun utbetalingFeilet(vedtak: Utbetalingsvedtak) = mapOf(
        "@event_name" to "utbetaling_feilet",
        "@vedtakId" to vedtak.vedtakId,
    ).toJson()

    private fun deserialiseringFeilet(vedtakJson: JsonNode) = mapOf(
        "@event_name" to "deserialisering_feilet",
        "@mottattVedtak" to vedtakJson,
    ).toJson()

    private fun utbetalingEksisterer(vedtak: Utbetalingsvedtak) = mapOf(
        "@event_name" to "utbetaling_eksisterer",
        "@vedtakId" to vedtak.vedtakId,
    ).toJson()

    private fun JsonMessage.correlationId(): String? = get("@correlation_id").textValue()

    companion object {
        private val logger = LoggerFactory.getLogger(VedtakMottaker::class.java)
    }
}

