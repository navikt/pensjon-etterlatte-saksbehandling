package no.nav.etterlatte.trygdetid.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.trygdetid.TrygdetidRepository
import no.nav.etterlatte.trygdetid.TrygdetidService

class ApplicationContext {
    val config: Config = ConfigFactory.load()
    val properties: ApplicationProperties = ApplicationProperties.fromEnv(System.getenv())
    val dataSource = DataSourceBuilder.createDataSource(
        jdbcUrl = properties.jdbcUrl,
        username = properties.dbUsername,
        password = properties.dbPassword
    )
    val trygdetidService = TrygdetidService(TrygdetidRepository(dataSource))
}