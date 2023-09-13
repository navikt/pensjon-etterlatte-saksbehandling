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

private const val UTF_8_WITH_PUA = 1208

interface EtterlatteJmsConnectionFactory {
    fun connection(): Connection
    fun start(listener: ExceptionListener, queue: String, messageListener: MessageListener)
    fun stop()
    fun send(xml: String, queue: String)
    fun sendMedSvar(xml: String, queue: String, replyQueue: String)
}

class JmsConnectionFactory(
    private val hostname: String,
    private val port: Int,
    private val queueManager: String,
    private val channel: String,
    private val username: String,
    private val password: String
) : EtterlatteJmsConnectionFactory {

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

    override fun connection(): Connection = connectionFactory.createConnection(username, password)

    override fun start(listener: ExceptionListener, queue: String, messageListener: MessageListener) {
        val connection = connection().apply { exceptionListener = listener }
        val session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)
        val consumer = session.createConsumer(session.createQueue(queue))
        consumer.messageListener = messageListener

        connection.start()
    }

    override fun stop() = connectionFactory.stop()

    override fun send(xml: String, queue: String) {
        val connection = connection()
        connection.createSession().use { session ->
            val producer = session.createProducer(session.createQueue(queue))
            val message = session.createTextMessage(xml)
            producer.send(message)
        }
    }

    override fun sendMedSvar(xml: String, queue: String, replyQueue: String) {
        connection().createSession().use { session ->
            val producer = session.createProducer(session.createQueue(queue))
            val message = session.createTextMessage(xml).apply {
                jmsReplyTo = session.createQueue(replyQueue)
            }
            producer.send(message)
        }
    }
}