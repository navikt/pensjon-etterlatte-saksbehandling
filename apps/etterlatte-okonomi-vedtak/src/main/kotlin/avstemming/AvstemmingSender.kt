package no.nav.etterlatte.avstemming

import no.nav.etterlatte.config.JmsConnectionFactory
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Avstemmingsdata
import org.slf4j.LoggerFactory

class AvstemmingSender(
    private val jmsConnectionFactory: JmsConnectionFactory,
    private val queue: String,
) {
    fun sendAvstemming(avstemmingsmelding: List<Avstemmingsdata>): Int {
        logger.info("Sender avstemming til Oppdrag")
        val connection = jmsConnectionFactory.connection()
        connection.createSession().use { session ->
            val producer = session.createProducer(session.createQueue(queue))
            var meldingerSendt = 0
            avstemmingsmelding.forEachIndexed { index, avstemmingsdata ->
                val message = session.createTextMessage(AvstemmingJaxb.toXml(avstemmingsdata))
                producer.send(message)
                meldingerSendt++
                logger.info("Avstemmingsmelding ${index+1} av ${avstemmingsmelding.size} overført til Oppdrag")
            }

            logger.info("Avstemming overført til Oppdrag")
            return meldingerSendt
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AvstemmingSender::class.java)
    }
}