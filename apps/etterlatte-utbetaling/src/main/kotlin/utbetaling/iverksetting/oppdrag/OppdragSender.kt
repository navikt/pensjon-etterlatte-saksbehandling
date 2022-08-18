package no.nav.etterlatte.utbetaling.iverksetting.oppdrag

import com.ibm.mq.jms.MQQueue
import no.nav.etterlatte.utbetaling.config.JmsConnectionFactory
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import org.slf4j.LoggerFactory

class OppdragSender(
    private val jmsConnectionFactory: JmsConnectionFactory,
    private val queue: String,
    private val replyQueue: String
) {
    fun sendOppdrag(oppdrag: Oppdrag): String {
        logger.info("Sender utbetaling til Oppdrag")
        logger.info(
            "Sender oppdrag for sakId=${oppdrag.oppdrag110.fagsystemId} med " +
                "vedtakId=${oppdrag.oppdrag110.oppdragsLinje150.first().vedtakId} til oppdrag"
        )

        val connection = jmsConnectionFactory.connection()
        return connection.createSession().use { session ->
            val producer = session.createProducer(session.createQueue(queue))
            val oppdragXml = OppdragJaxb.toXml(oppdrag)
            val message = session.createTextMessage(oppdragXml).apply {
                jmsReplyTo = MQQueue(replyQueue)
            }
            producer.send(message)
            logger.info("Utbetaling overført til Oppdrag")
            oppdragXml
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(OppdragSender::class.java)
    }
}