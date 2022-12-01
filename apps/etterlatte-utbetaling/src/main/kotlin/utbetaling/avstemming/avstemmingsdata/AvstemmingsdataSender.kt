package no.nav.etterlatte.utbetaling.grensesnittavstemming.avstemmingsdata

import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.etterlatte.utbetaling.config.JmsConnectionFactory
import no.nav.virksomhet.tjenester.avstemming.informasjon.konsistensavstemmingsdata.v1.Konsistensavstemmingsdata
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Avstemmingsdata
import org.slf4j.LoggerFactory

class AvstemmingsdataSender(
    private val jmsConnectionFactory: JmsConnectionFactory,
    private val queue: String
) {
    fun sendGrensesnittavstemming(avstemmingsdata: Avstemmingsdata): String {
        logger.info("Sender avstemmingsdata til Oppdrag")
        val xml = GrensesnittavstemmingsdataJaxb.toXml(avstemmingsdata)
        sendAvstemmingsdata(xml)
        logger.info("Grensesnittavstemmingsdata overført til Oppdrag", kv("avstemmingsdata", xml))
        return xml
    }

    fun sendKonsistensavstemming(avstemmingsdata: Konsistensavstemmingsdata): String {
        logger.info("Sender konsistensavstemmingsdata til Oppdrag")
        val xml = KonsistensavstemmingsdataJaxb.toXml(avstemmingsdata)
        sendAvstemmingsdata(xml)
        logger.info("Konsistensavstemmingsdata overført til Oppdrag", kv("avstemmingsdata", xml))
        return xml
    }

    private fun sendAvstemmingsdata(xml: String) {
        val connection = jmsConnectionFactory.connection()
        connection.createSession().use { session ->
            // Fjerner JMS-headers med targetClient=1
            val producer = session.createProducer(session.createQueue("queue:///$queue?targetClient=1"))
            val message = session.createTextMessage(xml)
            producer.send(message)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AvstemmingsdataSender::class.java)
    }
}