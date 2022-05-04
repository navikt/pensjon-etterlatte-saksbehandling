package no.nav.etterlatte.oppdrag


import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.domain.Utbetalingsoppdrag
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.Vedtak
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory


class VedtakMottaker(
    private val rapidsConnection: RapidsConnection,
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
            val vedtak: Vedtak = objectMapper.readValue(packet["@vedtak"].toJson())
            logger.info("Attestert vedtak med vedtakId=${vedtak.vedtakId} mottatt")

            try {
                if (oppdragService.oppdragEksistererFraFor(vedtak)) {
                    logger.info("Vedtak eksisterer fra før. Sendes ikke videre til oppdrag")
                    rapidsConnection.publish(utbetalingsoppdragEksisterer(vedtak))
                } else {
                    val attestasjon: Attestasjon = objectMapper.readValue(packet["@attestasjon"].toJson())
                    val utbetalingsoppdrag = oppdragService.opprettOgSendOppdrag(vedtak, attestasjon)
                    rapidsConnection.publish("key", utbetalingEvent(utbetalingsoppdrag))
                }
            } catch (e: Exception) {
                logger.error("En feil oppstod: ${e.message}", e)
                rapidsConnection.publish(utbetalingFeilet(vedtak))
                throw e
            }
        }


    private fun utbetalingEvent(utbetalingsoppdrag: Utbetalingsoppdrag) = mapOf(
        "@event_name" to "utbetaling_oppdatert",
        "@vedtakId" to utbetalingsoppdrag.utgaaendeOppdrag.vedtakId(),
        "@status" to utbetalingsoppdrag.status.name
    ).toJson()

    private fun utbetalingFeilet(vedtak: Vedtak) = mapOf(
        "@event_name" to "utbetaling_feilet",
        "@vedtakId" to vedtak.vedtakId,
    ).toJson()

    private fun utbetalingsoppdragEksisterer(vedtak: Vedtak) = mapOf(
        "@event_name" to "utbetaling_eksisterer",
        "@vedtakId" to vedtak.vedtakId,
    ).toJson()

    private fun JsonMessage.correlationId(): String? = get("@correlation_id").textValue()

    companion object {
        private val logger = LoggerFactory.getLogger(VedtakMottaker::class.java)
    }
}

