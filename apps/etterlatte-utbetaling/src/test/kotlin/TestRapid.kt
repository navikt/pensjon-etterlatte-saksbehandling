package no.nav.etterlatte

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.rapids_rivers.RapidsConnection

/**
 * Legger til notifyStartup og notifyShutdown
 */

class TestRapid : RapidsConnection() {
    private companion object {
        private val objectMapper =
            jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    private val messages = mutableListOf<Pair<String?, String>>()

    fun reset() {
        messages.clear()
    }

    fun sendTestMessage(message: String) {
        notifyMessage(message, this)
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

    override fun rapidName(): String {
        return "test-rapid"
    }

    override fun start() {
        notifyStartup()
    }

    override fun stop() {
        notifyShutdown()
    }
}
