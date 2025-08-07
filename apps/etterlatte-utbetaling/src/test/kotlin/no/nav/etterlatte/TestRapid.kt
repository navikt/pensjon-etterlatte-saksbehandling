package no.nav.etterlatte

import com.github.navikt.tbd_libs.rapids_and_rivers_api.FailedMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.OutgoingMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import com.github.navikt.tbd_libs.rapids_and_rivers_api.SentMessage
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

/**
 * Legger til notifyStartup og notifyShutdown
 */

class TestRapid : RapidsConnection() {
    private val messages = mutableListOf<Pair<String?, String>>()

    fun reset() {
        messages.clear()
    }

    fun sendTestMessage(message: String) {
//        notifyMessage(message, this, PrometheusMeterRegistry(PrometheusConfig.DEFAULT))
    }

    override fun publish(message: String) {
        messages.add(null to message)
    }

    override fun publish(
        key: String,
        message: String,
    ) {
        messages.add(key to message)
    }

    override fun publish(messages: List<OutgoingMessage>): Pair<List<SentMessage>, List<FailedMessage>> {
        TODO("Not yet implemented")
    }

    override fun rapidName(): String = "test-rapid"

    override fun start() {
        notifyStartup()
    }

    override fun stop() {
        notifyShutdown()
    }
}
