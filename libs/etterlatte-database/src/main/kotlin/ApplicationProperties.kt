package no.nav.etterlatte.libs.database

import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.database.DatabaseConfig.DB_DATABASE
import no.nav.etterlatte.libs.database.DatabaseConfig.DB_HOST
import no.nav.etterlatte.libs.database.DatabaseConfig.DB_JDBC_URL
import no.nav.etterlatte.libs.database.DatabaseConfig.DB_PASSWORD
import no.nav.etterlatte.libs.database.DatabaseConfig.DB_PORT
import no.nav.etterlatte.libs.database.DatabaseConfig.DB_USERNAME

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
                        env[DB_JDBC_URL] ?: jdbcUrl(
                            getValue(DB_HOST),
                            getValue(DB_PORT).toInt(),
                            getValue(DB_DATABASE),
                        ),
                    dbUsername = getValue(DB_USERNAME),
                    dbPassword = getValue(DB_PASSWORD),
                    httpPort = get("HTTP_PORT")?.toInt() ?: 8080,
                )
            }
    }
}
