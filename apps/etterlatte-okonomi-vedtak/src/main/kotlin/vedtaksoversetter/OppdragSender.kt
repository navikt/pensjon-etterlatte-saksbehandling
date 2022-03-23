package no.nav.etterlatte.vedtaksoversetter

import com.ibm.mq.jms.MQQueue
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import org.slf4j.LoggerFactory
import javax.jms.ConnectionFactory

class OppdragSender(
    val connectionFactory: ConnectionFactory,
    val queue: String,
    val replyQueue: String,
    val username: String,
    val password: String,
) {
    fun sendOppdrag(oppdrag: Oppdrag) {
        logger.info("Sender utbetaling til Oppdrag")
        connectionFactory.createConnection(username, password).use {
            logger.info("Connection til MQ opprettet")
            it.createSession().use { session ->
                val producer = session.createProducer(session.createQueue(queue))
                val message = session.createTextMessage(oppdrag.toXml()).apply {
                    jmsReplyTo = MQQueue(replyQueue)
                }

                logger.info("Legger melding på køen")
                producer.send(message)
                logger.info("Utbetaling overførert til Oppdrag")
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(OppdragSender::class.java)
    }
}