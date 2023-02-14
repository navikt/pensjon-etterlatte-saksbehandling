package no.nav.etterlatte

class FeatureToggleConfig {
    companion object {
        const val TREKK_I_LØPENDE_UTBETALING = "pensjon-etterlatte.trekk-i-loepende-utbetaling"
    }
}

enum class Properties(val navn: String) {
    ENABLED("funksjonsbrytere.enabled"),
    URI("funksjonsbrytere.uri"),
    CLUSTER("NAIS_CLUSTER_NAME"),
    APPLICATIONNAME("NAIS_APP_NAME")
}