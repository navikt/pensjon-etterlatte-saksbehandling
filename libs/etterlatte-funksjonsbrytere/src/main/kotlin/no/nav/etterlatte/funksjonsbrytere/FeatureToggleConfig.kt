package no.nav.etterlatte.funksjonsbrytere

import java.net.URI

interface FeatureToggle {
    fun key(): String
}

enum class FellesFeatureToggle(private val key: String) : FeatureToggle {
    NoOperationToggle("pensjon-etterlatte.no-operation-toggle");

    override fun key() = key
}

data class FeatureToggleProperties(
    val applicationName: String,
    val host: String,
    val apiKey: String
) {
    fun uri() = URI("$host/api")
}