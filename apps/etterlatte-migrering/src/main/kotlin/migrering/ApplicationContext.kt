package no.nav.etterlatte.migrering

import migrering.pen.PenKlient
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.ktor.httpClient

class ApplicationContext {
    private val properties: ApplicationProperties = ApplicationProperties.fromEnv(System.getenv())
    val dataSource = DataSourceBuilder.createDataSource(
        jdbcUrl = properties.jdbcUrl,
        username = properties.dbUsername,
        password = properties.dbPassword
    )

    val penklient = PenKlient(httpClient(), properties.penUrl)
}