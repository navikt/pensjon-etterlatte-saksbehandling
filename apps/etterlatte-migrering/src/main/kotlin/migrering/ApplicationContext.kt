package no.nav.etterlatte.migrering

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import migrering.verifisering.PDLKlient
import migrering.verifisering.Verifiserer
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleProperties
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.ktor.httpClient
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials
import no.nav.etterlatte.migrering.pen.PenKlient

internal class ApplicationContext {
    private val properties: ApplicationProperties = ApplicationProperties.fromEnv(System.getenv())
    val dataSource =
        DataSourceBuilder.createDataSource(
            jdbcUrl = properties.jdbcUrl,
            username = properties.dbUsername,
            password = properties.dbPassword,
        )

    private val config = ConfigFactory.load()
    val penklient = PenKlient(config, httpClient())

    val featureToggleService: FeatureToggleService =
        FeatureToggleService.initialiser(featureToggleProperties(ConfigFactory.load()))
    val pesysRepository = PesysRepository(dataSource)

    val pdlKlient =
        PDLKlient(
            config,
            httpClientClientCredentials(
                azureAppClientId = config.getString("azure.app.client.id"),
                azureAppJwk = config.getString("azure.app.jwk"),
                azureAppWellKnownUrl = config.getString("azure.app.well.known.url"),
                azureAppScope = config.getString("pdl.azure.scope"),
            ),
        )
    val verifiserer = Verifiserer(pdlKlient = pdlKlient, repository = pesysRepository)
}

private fun featureToggleProperties(config: Config) =
    FeatureToggleProperties(
        applicationName = config.getString("funksjonsbrytere.unleash.applicationName"),
        host = config.getString("funksjonsbrytere.unleash.host"),
        apiKey = config.getString("funksjonsbrytere.unleash.token"),
    )
