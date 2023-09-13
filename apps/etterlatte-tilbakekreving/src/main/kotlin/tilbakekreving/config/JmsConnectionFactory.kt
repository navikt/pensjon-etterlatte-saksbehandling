package no.nav.etterlatte.tilbakekreving.config

import com.ibm.mq.MQC
import com.ibm.mq.jakarta.jms.MQConnectionFactory
import com.ibm.msg.client.jakarta.jms.JmsConstants
import com.ibm.msg.client.jakarta.wmq.WMQConstants
import jakarta.jms.Connection
import jakarta.jms.ExceptionListener
import jakarta.jms.MessageListener
import jakarta.jms.Session
import org.messaginghub.pooled.jms.JmsPoolConnectionFactory
import org.slf4j.LoggerFactory

class JmsConnectionFactory(
    private val hostname: String,
    private val port: Int,
    private val queueManager: String,
    private val channel: String,
    private val username: String,
    private val password: String
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private val connectionFactory = MQConnectionFactory().also {
        it.hostName = hostname
        it.port = port
        it.queueManager = queueManager
        it.channel = channel
        it.transportType = WMQConstants.WMQ_CM_CLIENT
        it.ccsid = UTF_8_WITH_PUA

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

    fun start(listener: ExceptionListener, queue: String, messageListener: MessageListener, loggtekst: String) {
        val connection = connection().apply { exceptionListener = listener }
        val session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)
        val consumer = session.createConsumer(session.createQueue(queue))
        consumer.messageListener = messageListener

        connection.start()
        logger.info(loggtekst)
    }

    fun stop() = connectionFactory.stop()

    private companion object {
        const val UTF_8_WITH_PUA = 1208
    }
}