package no.nav.etterlatte.tilbakekreving

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.tilbakekreving.config.JmsConnectionFactory
import no.nav.etterlatte.tilbakekreving.oppdrag.KravgrunnlagJaxb
import org.slf4j.LoggerFactory
import javax.jms.ExceptionListener
import javax.jms.Message
import javax.jms.MessageListener
import javax.jms.Session

class TilbakekrevingConsumer(
    private val tilbakekrevingService: TilbakekrevingService,
    jmsConnectionFactory: JmsConnectionFactory,
    queue: String,
) : MessageListener {

    init {
        val connection = jmsConnectionFactory.connection().apply {
            exceptionListener = ExceptionListener {
                logger.error("En feil oppstod med tilkobling mot MQ: ${it.message}", it)
            }
        }.also { it.start() }

        val session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)
        val consumer = session.createConsumer(session.createQueue(queue))
        consumer.messageListener = this
    }


    override fun onMessage(message: Message?) {
        withLogContext {
            runBlocking {
                var kravgrunnlagXml: String? = null
                try {
                    logger.info("Tilbakekrevings grunnlagg mottatt ")
                    kravgrunnlagXml = message!!.getBody(String::class.java)
                    val kravgrunnlag = KravgrunnlagJaxb.toKravgrunnlag(kravgrunnlagXml)
                    tilbakekrevingService.lagreKravgrunnlag(kravgrunnlag)
                } catch (e: Exception) {
                    logger.error("Feilet under mottak av tilbakekrevingsgrunnlag", e)
                }
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TilbakekrevingConsumer::class.java)
    }

}