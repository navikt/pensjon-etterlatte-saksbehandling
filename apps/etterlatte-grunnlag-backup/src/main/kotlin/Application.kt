package no.nav.etterlatte

import com.typesafe.config.ConfigFactory
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.database.ApplicationProperties
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.ktor.initialisering.initEmbeddedServer
import no.nav.etterlatte.libs.ktor.initialisering.run

fun main() {
    val properties: ApplicationProperties = ApplicationProperties.fromEnv(Miljoevariabler.systemEnv())

    val ds =
        DataSourceBuilder.createDataSource(
            jdbcUrl = properties.jdbcUrl,
            username = properties.dbUsername,
            password = properties.dbPassword,
        )

    initEmbeddedServer(
        httpPort = 8080,
        applicationConfig = ConfigFactory.load(),
    ) {
        grunnlagRoute(
            dao = OpplysningDao(ds),
        )
    }.run()
}
