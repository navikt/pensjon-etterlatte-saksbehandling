package no.nav.etterlatte.funksjonsbrytere

interface FeatureToggle {
    fun key(): String
}

enum class FellesFeatureToggle(private val key: String) : FeatureToggle {
    NoOperationToggle("pensjon-etterlatte.no-operation-toggle");

    override fun key() = key
}

enum class FeatureToggleServiceProperties(val navn: String) {
    ENABLED("funksjonsbrytere.enabled"),
    URI("funksjonsbrytere.uri"),
    CLUSTER("NAIS_CLUSTER_NAME"),
    APPLICATIONNAME("NAIS_APP_NAME")
}