package no.nav.etterlatte.migrering

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleProperties
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.ktor.httpClient
import no.nav.etterlatte.migrering.pen.PenKlient
import java.net.URI

internal class ApplicationContext {
    private val properties: ApplicationProperties = ApplicationProperties.fromEnv(System.getenv())
    val dataSource = DataSourceBuilder.createDataSource(
        jdbcUrl = properties.jdbcUrl,
        username = properties.dbUsername,
        password = properties.dbPassword
    )

    private val config = ConfigFactory.load()
    val penklient = PenKlient(config, httpClient())

    private val featureToggleService: FeatureToggleService =
        FeatureToggleService.initialiser(featureToggleProperties(ConfigFactory.load()))
    val pesysRepository = PesysRepository(dataSource)
    val sakmigrerer = Sakmigrerer(pesysRepository, featureToggleService)
}

private fun featureToggleProperties(config: Config) = FeatureToggleProperties(
    applicationName = config.getString("funksjonsbrytere.unleash.applicationName"),
    uri = URI(config.getString("funksjonsbrytere.unleash.uri")),
    cluster = config.getString("funksjonsbrytere.unleash.cluster")
)