package no.nav.etterlatte.config

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
        it.channel =  channel
        it.transportType = WMQConstants.WMQ_CM_CLIENT
        it.ccsid = UTF_8_WITH_PUA

        // TODO trenger man dette?
        //it.clientReconnectOptions = WMQConstants.WMQ_CLIENT_RECONNECT
        //it.clientReconnectTimeout = 600

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