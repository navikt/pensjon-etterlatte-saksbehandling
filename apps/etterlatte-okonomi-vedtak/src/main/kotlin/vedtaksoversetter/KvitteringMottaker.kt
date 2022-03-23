package no.nav.etterlatte.vedtaksoversetter


import com.ibm.mq.jms.MQQueue
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import javax.jms.ConnectionFactory


internal class KvitteringMottaker(
    rapidsConnection: RapidsConnection,
    connectionFactory: ConnectionFactory,
    queue: String,
    username: String,
    password: String,
) {

    private val logger = LoggerFactory.getLogger(KvitteringMottaker::class.java)

    init {
        withLogContext {
            connectionFactory.createConnection(username, password).use { connection ->
                connection.createSession().use { session ->
                    val consumer = session.createConsumer(MQQueue(queue))
                    consumer.setMessageListener { message ->
                        val body = message.getBody(String::class.java)
                        logger.info("Kvittering mottatt", kv("body", body))

                        //rapidsConnection.publish("")
                    }
                }
            }
        }
    }

}

