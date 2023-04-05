package no.nav.etterlatte.migrering

import no.nav.etterlatte.libs.database.jdbcUrl

class ApplicationProperties(
    val jdbcUrl: String,
    val dbUsername: String,
    val dbPassword: String
) {
    companion object {
        fun fromEnv(env: Map<String, String>) = env.run {
            ApplicationProperties(
                jdbcUrl = env["DB_JDBC_URL"] ?: jdbcUrl(
                    value("DB_HOST"),
                    value("DB_PORT").toInt(),
                    value("DB_DATABASE")
                ),
                dbUsername = value("DB_USERNAME"),
                dbPassword = value("DB_PASSWORD")
            )
        }

        private fun Map<String, String>.value(property: String): String =
            requireNotNull(this[property]) { "Property $property was null" }
    }
}