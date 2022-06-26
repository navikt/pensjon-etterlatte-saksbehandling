package no.nav.etterlatte.tilbakekreving.kravgrunnlag

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.tilbakekreving.Tilbakekreving
import no.nav.etterlatte.tilbakekreving.TilbakekrevingService
import no.nav.etterlatte.tilbakekreving.config.JmsConnectionFactory
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import javax.jms.ExceptionListener
import javax.jms.Message
import javax.jms.MessageListener
import javax.jms.Session
import kotlin.system.exitProcess

data class TilbakekrevingEvent(
    @JsonProperty("@event") val event: String,
    @JsonProperty("@tilbakekreving") val tilbakekreving: Map<String, Any>, // TODO finne ut hva som bør sendes her
)

class KravgrunnlagConsumer(
    private val rapidsConnection: RapidsConnection,
    private val tilbakekrevingService: TilbakekrevingService,
    private val jmsConnectionFactory: JmsConnectionFactory,
    private val queue: String,
) : MessageListener {

    fun start() {
        val connection = jmsConnectionFactory.connection()
            .apply {
                exceptionListener = ExceptionListener {
                    logger.error("En feil oppstod med tilkobling mot MQ: ${it.message}", it)
                    exitProcess(-1) // Restarter appen
                }
            }
            .also { it.start() }

        val session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)
        val consumer = session.createConsumer(session.createQueue(queue))
        consumer.messageListener = this
    }

    override fun onMessage(message: Message) {
        withLogContext {
            val kravgrunnlagXml: String?
            try {
                logger.info("Kravgrunnlag for tilbakekreving mottatt - levert ${message.deliveryCount()} ganger")
                kravgrunnlagXml = message.getBody(String::class.java)
                val detaljertKravgrunnlag = KravgrunnlagJaxb.toDetaljertKravgrunnlagDto(kravgrunnlagXml)
                val tilbakekreving =
                    tilbakekrevingService.opprettTilbakekrevingFraKravgrunnlag(detaljertKravgrunnlag, kravgrunnlagXml)
                logger.info("Sender event om mottatt kravgrunnlag")
                sendMottattKravgrunnlagEvent(tilbakekreving)
            } catch (t: Throwable) {
                // Log error and throw to trigger redelivery.
                // Message will be added to the backout queue after x deliveries
                logger.error("Feilet under mottak av kravgrunnlag", t)
                throw t
            }
        }
    }

    private fun sendMottattKravgrunnlagEvent(tilbakekreving: Tilbakekreving.MottattKravgrunnlag) {
        rapidsConnection.publish(
            key = tilbakekreving.sakId.value.toString(),
            message = TilbakekrevingEvent(
                event = "TILBAKEKREVING:MOTTATT_KRAVGRUNNLAG",
                tilbakekreving = mapOf(
                    "kravgrunnlagId" to tilbakekreving.kravgrunnlagId.value,
                    "behandlingId" to tilbakekreving.behandlingId.value
                )
            ).toJson()
        )
    }

    fun Message.deliveryCount() = this.getLongProperty("JMSXDeliveryCount")

    companion object {
        private val logger = LoggerFactory.getLogger(KravgrunnlagConsumer::class.java)
    }

}