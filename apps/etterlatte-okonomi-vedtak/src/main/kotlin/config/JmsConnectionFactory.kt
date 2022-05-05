package no.nav.etterlatte.utbetaling.config

import com.ibm.mq.MQC
import com.ibm.mq.jms.MQConnectionFactory
import com.ibm.msg.client.jms.JmsConstants
import com.ibm.msg.client.wmq.WMQConstants
import org.messaginghub.pooled.jms.JmsPoolConnectionFactory
import javax.jms.Connection


private const val UTF_8_WITH_PUA = 1208

class JmsConnectionFactory(
    private val hostname: String,
    private val port: Int,
    private val queueManager: String,
    private val channel: String,
    private val username: String,
    private val password: String,
) {
    private val connectionFactory = MQConnectionFactory().also {
        it.hostName = hostname
        it.port = port
        it.queueManager = queueManager
        it.channel = channel
        it.transportType = WMQConstants.WMQ_CM_CLIENT
        it.ccsid = UTF_8_WITH_PUA

        // TODO fjernet disse da de gjorde at ting ikke fungerte i tester
        //it.clientReconnectOptions =
        //    WMQConstants.WMQ_CLIENT_RECONNECT // https://www.ibm.com/docs/en/ibm-mq/7.5?topic=objects-clientreconnectoptions
        //it.clientReconnectTimeout =
        //    600 // default 1800 - https://www.ibm.com/docs/en/ibm-mq/7.5?topic=objects-clientreconnecttimeout


        it.setBooleanProperty(JmsConstants.USER_AUTHENTICATION_MQCSP, true)
        it.setIntProperty(WMQConstants.JMS_IBM_CHARACTER_SET, UTF_8_WITH_PUA)
        it.setIntProperty(WMQConstants.JMS_IBM_ENCODING, MQC.MQENC_NATIVE)
    }.let {
        val pooledConnectionFactory = JmsPoolConnectionFactory()
        pooledConnectionFactory.connectionFactory = it
        pooledConnectionFactory.maxConnections = 1
        pooledConnectionFactory
    }

    fun connection(): Connection = connectionFactory.createConnection(username, password)

    fun stop() = connectionFactory.stop()
}