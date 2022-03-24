package no.nav.etterlatte.vedtaksoversetter

import com.ibm.mq.jms.MQQueue
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import org.slf4j.LoggerFactory
import javax.jms.Connection

class OppdragSender(
    val jmsConnection: Connection,
    val queue: String,
    val replyQueue: String,
) {
    fun sendOppdrag(oppdrag: Oppdrag) {
        logger.info("Sender utbetaling til Oppdrag")
        jmsConnection.createSession().use { session ->
            val producer = session.createProducer(session.createQueue(queue))
            val message = session.createTextMessage(oppdrag.toXml()).apply {
                jmsReplyTo = MQQueue(replyQueue)
            }

            logger.info("Legger melding på køen")
            producer.send(message)
            logger.info("Utbetaling overførert til Oppdrag")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(OppdragSender::class.java)
    }
}