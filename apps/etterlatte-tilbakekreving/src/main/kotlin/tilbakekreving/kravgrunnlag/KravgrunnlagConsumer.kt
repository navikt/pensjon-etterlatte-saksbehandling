package no.nav.etterlatte.tilbakekreving.kravgrunnlag

import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.tilbakekreving.config.JmsConnectionFactory
import org.slf4j.LoggerFactory
import javax.jms.ExceptionListener
import javax.jms.Message
import javax.jms.MessageListener
import javax.jms.Session

class KravgrunnlagConsumer(
    private val tilbakekrevingService: TilbakekrevingService,
    private val jmsConnectionFactory: JmsConnectionFactory,
    private val queue: String,
) : MessageListener {

    fun start() {
        val connection = jmsConnectionFactory.connection().apply {
            exceptionListener = ExceptionListener {
                logger.error("En feil oppstod med tilkobling mot MQ: ${it.message}", it)
            }
        }.also { it.start() }

        val session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)
        val consumer = session.createConsumer(session.createQueue(queue))
        consumer.messageListener = this
    }

    override fun onMessage(message: Message) {
        withLogContext {
            val kravgrunnlagXml: String?
            try {
                logger.info("Kravgrunnlag mottatt")
                kravgrunnlagXml = message.getBody(String::class.java)
                val kravgrunnlag = KravgrunnlagJaxb.toKravgrunnlag(kravgrunnlagXml)
                tilbakekrevingService.lagreKravgrunnlag(kravgrunnlag)
            } catch (e: Throwable) {
                logger.error("Feilet under mottak av kravgrunnlag", e)
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(KravgrunnlagConsumer::class.java)
    }

}