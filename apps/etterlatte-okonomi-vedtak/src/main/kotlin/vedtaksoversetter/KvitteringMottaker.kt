package no.nav.etterlatte.vedtaksoversetter


import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import javax.jms.Connection
import javax.jms.Session


internal class KvitteringMottaker(
    rapidsConnection: RapidsConnection,
    jmsConnection: Connection,
    queue: String,
) {

    private val logger = LoggerFactory.getLogger(KvitteringMottaker::class.java)

    private val session = jmsConnection.createSession(false, Session.AUTO_ACKNOWLEDGE)
    private val consumer = session.createConsumer(session.createQueue(queue))

    init {
        withLogContext {
            logger.info("Setting message listener")
            logger.info("Connection:$jmsConnection")
            consumer.setMessageListener { message ->
               try {
                   logger.info("Kvittering motatt - leser body")
                   val body = message.getBody(String::class.java)
                   logger.info("Kvittering mottatt", kv("body", body))
               } catch (t: Throwable) {
                   logger.error("Feilet under mottak av melding")
               }
            //rapidsConnection.publish("")
            }
        }
    }

}

