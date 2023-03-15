package no.nav.etterlatte.trygdetid.config

class ApplicationProperties(
    val httpPort: Int
) {
    companion object {
        fun fromEnv(env: Map<String, String>) = env.run {
            ApplicationProperties(
                httpPort = valueOrNull("HTTP_PORT")?.toInt() ?: 8080
            )
        }

        private fun Map<String, String>.value(property: String): String =
            requireNotNull(this[property]) { "Property $property was null" }

        private fun Map<String, String>.valueOrNull(property: String): String? =
            this[property]
    }
}