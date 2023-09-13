package no.nav.etterlatte.utbetaling

import jakarta.jms.Connection
import jakarta.jms.ExceptionListener
import jakarta.jms.MessageListener
import no.nav.etterlatte.utbetaling.config.EtterlatteJmsConnectionFactory

class DummyJmsConnectionFactory : EtterlatteJmsConnectionFactory {
    override fun connection(): Connection {
        TODO("Not yet implemented")
    }

    override fun start(listener: ExceptionListener, queue: String, messageListener: MessageListener) {
        TODO("Not yet implemented")
    }

    override fun stop() {
    }

    override fun send(xml: String, queue: String) {
    }

    override fun sendMedSvar(xml: String, queue: String, replyQueue: String) {
    }
}