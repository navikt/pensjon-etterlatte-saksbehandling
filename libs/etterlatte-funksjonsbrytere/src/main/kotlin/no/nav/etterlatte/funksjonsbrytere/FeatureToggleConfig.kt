package no.nav.etterlatte.funksjonsbrytere

class FeatureToggleConfig {
    companion object {
        const val NoOperationToggle = "pensjon-etterlatte.no-operation-toggle"
    }
}

enum class Properties(val navn: String) {
    ENABLED("funksjonsbrytere.enabled"),
    URI("funksjonsbrytere.uri"),
    CLUSTER("NAIS_CLUSTER_NAME"),
    APPLICATIONNAME("NAIS_APP_NAME")
}