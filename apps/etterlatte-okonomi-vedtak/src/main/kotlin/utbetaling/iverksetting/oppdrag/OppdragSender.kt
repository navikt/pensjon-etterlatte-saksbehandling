package no.nav.etterlatte.utbetaling.iverksetting.oppdrag

import com.ibm.mq.jms.MQQueue
import no.nav.etterlatte.utbetaling.config.JmsConnectionFactory
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import org.slf4j.LoggerFactory

class OppdragSender(
    private val jmsConnectionFactory: JmsConnectionFactory,
    private val queue: String,
    private val replyQueue: String,
) {
    fun sendOppdrag(oppdrag: Oppdrag) {
        logger.info("Sender utbetaling til Oppdrag")
        val connection = jmsConnectionFactory.connection()
        connection.createSession().use { session ->
            val producer = session.createProducer(session.createQueue(queue))
            val message = session.createTextMessage(OppdragJaxb.toXml(oppdrag)).apply {
                jmsReplyTo = MQQueue(replyQueue)
            }
            producer.send(message)
            logger.info("Utbetaling overført til Oppdrag")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(OppdragSender::class.java)
    }
}