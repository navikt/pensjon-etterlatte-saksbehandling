package no.nav.etterlatte.vedtaksvurdering.config

class ApplicationProperties(
    val jdbcUrl: String,
    val dbUsername: String,
    val dbPassword: String,
    val behandlingScope: String,
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
                    behandlingScope = value("BEHANDLING_AZURE_SCOPE"),
                )
            }

        private fun Map<String, String>.value(property: String): String = requireNotNull(this[property]) { "Property $property was null" }
    }
}

private fun jdbcUrl(
    host: String,
    port: Int,
    databaseName: String,
) = "jdbc:postgresql://$host:$port/$databaseName"
