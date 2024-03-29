package no.nav.etterlatte.libs.database

class ApplicationProperties(
    val jdbcUrl: String,
    val dbUsername: String,
    val dbPassword: String,
    val httpPort: Int,
) {
    companion object {
        fun fromEnv(env: Map<String, String>) =
            env.run {
                ApplicationProperties(
                    jdbcUrl =
                        env["DB_JDBC_URL"] ?: jdbcUrl(
                            value("DB_HOST"),
                            value("DB_PORT").toInt(),
                            value("DB_DATABASE"),
                        ),
                    dbUsername = value("DB_USERNAME"),
                    dbPassword = value("DB_PASSWORD"),
                    httpPort = valueOrNull("HTTP_PORT")?.toInt() ?: 8080,
                )
            }

        private fun Map<String, String>.value(property: String): String = requireNotNull(this[property]) { "Property $property was null" }

        private fun Map<String, String>.valueOrNull(property: String): String? = this[property]
    }
}
