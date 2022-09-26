package no.nav.etterlatte.vilkaarsvurdering.config

import com.typesafe.config.ConfigFactory
import io.ktor.server.auth.AuthenticationConfig
import io.ktor.server.config.HoconApplicationConfig
import no.nav.security.token.support.v2.tokenValidationSupport

class ApplicationContext(
    val properties: ApplicationProperties = ApplicationProperties.fromEnv(System.getenv())
) {
    var dataSourceBuilder = DataSourceBuilder(
        jdbcUrl = jdbcUrl(
            host = properties.dbHost,
            port = properties.dbPort,
            databaseName = properties.dbName
        ),
        username = properties.dbUsername,
        password = properties.dbPassword
    )

    var dataSource = dataSourceBuilder.dataSource()

    var tokenValidering: AuthenticationConfig.() -> Unit =
        { tokenValidationSupport(config = HoconApplicationConfig(ConfigFactory.load())) }
}

private fun jdbcUrl(host: String, port: Int, databaseName: String) =
    "jdbc:postgresql://$host:$port/$databaseName"