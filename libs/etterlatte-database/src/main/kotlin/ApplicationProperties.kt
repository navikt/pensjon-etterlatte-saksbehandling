package no.nav.etterlatte.libs.database

import no.nav.etterlatte.libs.common.Miljoevariabler

class ApplicationProperties(
    val jdbcUrl: String,
    val dbUsername: String,
    val dbPassword: String,
    val httpPort: Int,
) {
    companion object {
        fun fromEnv(env: Miljoevariabler) =
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
                    httpPort = get("HTTP_PORT")?.toInt() ?: 8080,
                )
            }
    }
}
