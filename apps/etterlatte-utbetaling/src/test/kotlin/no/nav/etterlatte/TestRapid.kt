package no.nav.etterlatte

import no.nav.helse.rapids_rivers.RapidsConnection

/**
 * Legger til notifyStartup og notifyShutdown
 */

class TestRapid : RapidsConnection() {
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

    override fun rapidName(): String = "test-rapid"

    override fun start() {
        notifyStartup()
    }

    override fun stop() {
        notifyShutdown()
    }
}
