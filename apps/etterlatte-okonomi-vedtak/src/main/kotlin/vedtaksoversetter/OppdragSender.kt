package no.nav.etterlatte.vedtaksoversetter

import com.ibm.mq.jms.MQQueue
import no.nav.etterlatte.common.Jaxb
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import org.slf4j.LoggerFactory
import javax.jms.Connection

class OppdragSender(
    val jmsConnection: Connection,
    val queue: String,
    val replyQueue: String,
) {
    fun sendOppdrag(oppdrag: Oppdrag) {
        logger.info("Sender utbetalingsoppdrag til Oppdrag")
        jmsConnection.createSession().use { session ->
            val producer = session.createProducer(session.createQueue(queue))
            val message = session.createTextMessage(Jaxb.toXml(oppdrag)).apply {
                jmsReplyTo = MQQueue(replyQueue)
            }
            producer.send(message)
            logger.info("Utbetalingsoppdrag overførert til Oppdrag")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(OppdragSender::class.java)
    }
}