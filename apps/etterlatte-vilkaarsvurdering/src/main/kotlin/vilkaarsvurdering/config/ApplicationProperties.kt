package no.nav.etterlatte.vilkaarsvurdering.config

data class ApplicationProperties(
    val dbName: String,
    val dbHost: String,
    val dbPort: Int,
    val dbUsername: String,
    val dbPassword: String
) {
    companion object {
        fun fromEnv(env: Map<String, String>) = env.run {
            ApplicationProperties(
                dbName = value("DB_DATABASE"),
                dbHost = value("DB_HOST"),
                dbPort = value("DB_PORT").toInt(),
                dbUsername = value("DB_USERNAME"),
                dbPassword = value("DB_PASSWORD")
            )
        }

        private fun Map<String, String>.value(property: String): String =
            requireNotNull(this[property]) { "Property $property was null" }
    }
}