package no.nav.etterlatte.tilbakekreving.kravgrunnlag

import jakarta.jms.ExceptionListener
import jakarta.jms.Message
import jakarta.jms.MessageListener
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.tilbakekreving.config.EtterlatteJmsConnectionFactory
import no.nav.etterlatte.tilbakekreving.kravgrunnlag.KravgrunnlagJaxb.toDetaljertKravgrunnlagDto
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class KravgrunnlagConsumer(
    private val connectionFactory: EtterlatteJmsConnectionFactory,
    private val queue: String,
    private val kravgrunnlagService: KravgrunnlagService
) : MessageListener {

    fun start() = connectionFactory.start(
        listener = exceptionListener(),
        queue = queue,
        messageListener = this
    ).also { logger.info("Lytter på kravgrunnlag fra tilbakekrevingskomponenten") }

    override fun onMessage(message: Message) = withLogContext {
        var kravgrunnlagPayload: String? = null
        try {
            logger.info("Kravgrunnlag (id=${message.jmsMessageID}) mottatt ${message.deliveryCount()} gang(er)")
            kravgrunnlagPayload = message.getBody(String::class.java)
            val detaljertKravgrunnlag = toDetaljertKravgrunnlagDto(kravgrunnlagPayload)
            kravgrunnlagService.opprettTilbakekreving(detaljertKravgrunnlag)
        } catch (t: Throwable) {
            logger.error("Feilet under mottak av kravgrunnlag (Sjekk sikkerlogg for payload", t)
            sikkerLogg.error("Feilet under mottak av kravgrunnlag", kv("kravgrunnlag", kravgrunnlagPayload), t)

            // Exception trigger retry - etter x forsøk vil meldingen legges på backout kø
            throw t
        }
    }

    private fun exceptionListener() = ExceptionListener {
        logger.error("En feil oppstod med tilkoblingen mot tilbakekrevingskomponenten: ${it.message}", it)
    }

    private fun Message.deliveryCount() = this.getLongProperty("JMSXDeliveryCount")

    companion object {
        private val logger = LoggerFactory.getLogger(KravgrunnlagConsumer::class.java)
        private val sikkerLogg: Logger = LoggerFactory.getLogger("sikkerLogg")
    }
}