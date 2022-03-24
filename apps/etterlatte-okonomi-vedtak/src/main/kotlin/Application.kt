package no.nav.etterlatte

import com.ibm.mq.MQC
import com.ibm.mq.jms.MQConnectionFactory
import com.ibm.msg.client.jms.JmsConstants
import com.ibm.msg.client.wmq.WMQConstants
import no.nav.etterlatte.vedtaksoversetter.KvitteringMottaker
import no.nav.etterlatte.vedtaksoversetter.OppdragMapper
import no.nav.etterlatte.vedtaksoversetter.OppdragSender
import no.nav.etterlatte.vedtaksoversetter.Vedtaksoversetter
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import javax.jms.ConnectionFactory

private const val UTF_8_WITH_PUA = 1208

fun main() {
    val env = System.getenv().toMutableMap().apply {
        put("KAFKA_CONSUMER_GROUP_ID", this.required("NAIS_APP_NAME").replace("-", ""))
    }

    val logger = LoggerFactory.getLogger(OppdragSender::class.java)

    val jmsConnection = connectionFactory(env)
        .createConnection(env.required("srvuser"), env.required("srvpwd"))

    val oppdragSender = OppdragSender(
        jmsConnection = jmsConnection,
        queue = env.required("OPPDRAG_SEND_MQ_NAME"),
        replyQueue = env.required("OPPDRAG_KVITTERING_MQ_NAME"),
    )

    RapidApplication.create(env)
        .apply {
            KvitteringMottaker(
                rapidsConnection = this,
                jmsConnection = jmsConnection,
                queue = env.required("OPPDRAG_KVITTERING_MQ_NAME"),
            )
            Vedtaksoversetter(
                rapidsConnection = this,
                oppdragMapper = OppdragMapper,
                oppdragSender = oppdragSender
            )

            register(object : RapidsConnection.StatusListener {
                override fun onStartup(rapidsConnection: RapidsConnection) {
                    logger.info("Starter jms connection")
                    jmsConnection.start()
                }

                override fun onShutdown(rapidsConnection: RapidsConnection) {
                    jmsConnection.close()
                }
            })
        }.start()
}

private fun connectionFactory(env: Map<String, String>): ConnectionFactory =
    MQConnectionFactory().apply {
        hostName = env.required("OPPDRAG_MQ_HOSTNAME")
        port = env.required("OPPDRAG_MQ_PORT").toInt()
        queueManager = env.required("OPPDRAG_MQ_MANAGER")
        channel =  env.required("OPPDRAG_MQ_CHANNEL")
        transportType = WMQConstants.WMQ_CM_CLIENT
        ccsid = UTF_8_WITH_PUA

        setBooleanProperty(JmsConstants.USER_AUTHENTICATION_MQCSP, true)
        setIntProperty(WMQConstants.JMS_IBM_ENCODING, MQC.MQENC_NATIVE)
        setIntProperty(WMQConstants.JMS_IBM_CHARACTER_SET, UTF_8_WITH_PUA)
    }

private fun Map<String, String>.required(property: String): String =
    requireNotNull(this[property]) { "Property $property was null" }
