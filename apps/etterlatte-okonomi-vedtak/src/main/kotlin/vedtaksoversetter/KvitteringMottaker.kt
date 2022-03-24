package no.nav.etterlatte.vedtaksoversetter


import com.ibm.mq.jms.MQQueue
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

    init {
        withLogContext {
            logger.info("Connection created")
            jmsConnection.createSession(false, Session.AUTO_ACKNOWLEDGE).use { session ->
                logger.info("Session created")
                val consumer = session.createConsumer(session.createQueue(queue))
                logger.info("Consumer created")
                consumer.setMessageListener { message ->
                    logger.info("Kvittering motatt - leser body")
                    val body = message.getBody(String::class.java)
                    logger.info("Kvittering mottatt", kv("body", body))

                    //rapidsConnection.publish("")
                }
            }
        }
    }

}

