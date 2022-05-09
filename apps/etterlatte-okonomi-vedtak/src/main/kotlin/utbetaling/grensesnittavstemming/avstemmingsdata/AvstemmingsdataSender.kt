package no.nav.etterlatte.utbetaling.grensesnittavstemming.avstemmingsdata

import no.nav.etterlatte.utbetaling.config.JmsConnectionFactory
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Avstemmingsdata
import org.slf4j.LoggerFactory

class AvstemmingsdataSender(
    private val jmsConnectionFactory: JmsConnectionFactory,
    private val queue: String,
) {
    fun sendAvstemming(avstemmingsdata: Avstemmingsdata): String {
        logger.info("Sender avstemmingsdata til Oppdrag")
        val connection = jmsConnectionFactory.connection()
        connection.createSession().use { session ->
            val producer = session.createProducer(session.createQueue(queue))
            val xml = AvstemmingsdataJaxb.toXml(avstemmingsdata)
            val message = session.createTextMessage(xml)
            producer.send(message)
            logger.info("Avstemmingsdata overført til Oppdrag")
            return xml
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AvstemmingsdataSender::class.java)
    }
}