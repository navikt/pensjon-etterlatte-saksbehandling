package no.nav.etterlatte.tilbakekreving

import io.mockk.every
import io.mockk.mockk
import jakarta.jms.Connection
import jakarta.jms.ExceptionListener
import jakarta.jms.Message
import jakarta.jms.MessageListener
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.retry
import no.nav.etterlatte.tilbakekreving.config.EtterlatteJmsConnectionFactory

class DummyJmsConnectionFactory : EtterlatteJmsConnectionFactory {

    private var mq: MutableMap<String, String> = mutableMapOf()
    private var listeners = mutableMapOf<String, List<MessageListener>>()
    private var replyListeners = mutableMapOf<String, List<MessageListener>>()

    override fun connection(): Connection = mockk()

    override fun start(listener: ExceptionListener, queue: String, messageListener: MessageListener) {
        listeners[queue] = listOf(messageListener)
        replyListeners[queue] = listOf(messageListener)
    }

    override fun stop() {
    }

    override fun send(xml: String, queue: String) {
        mq[queue] = xml
        listeners[queue]?.forEach {
            runBlocking {
                retry(3) {
                    it.onMessage(
                        mockk<Message>().also {
                            every { it.jmsMessageID } returns System.currentTimeMillis().toString()
                            every { it.getBody(String::class.java) } returns xml
                            every { it.getLongProperty(any()) } returns 1L
                        }
                    )
                }
            }
        }
    }

    override fun sendMedSvar(xml: String, queue: String, replyQueue: String) {
        send(xml, queue)
    }
}