package no.nav.etterlatte.avstemming

import no.nav.etterlatte.config.JmsConnectionFactory
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Avstemmingsdata
import org.slf4j.LoggerFactory

class AvstemmingSender(
    private val jmsConnectionFactory: JmsConnectionFactory,
    private val queue: String,
) {
    fun sendAvstemming(avstemmingsdata: Avstemmingsdata) {
        logger.info("Sender avstemmingsdata til Oppdrag")
        val connection = jmsConnectionFactory.connection()
        connection.createSession().use { session ->
            val producer = session.createProducer(session.createQueue(queue))
            val message = session.createTextMessage(AvstemmingJaxb.toXml(avstemmingsdata))
            producer.send(message)
            logger.info("Avstemmingsdata overført til Oppdrag")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AvstemmingSender::class.java)
    }
}