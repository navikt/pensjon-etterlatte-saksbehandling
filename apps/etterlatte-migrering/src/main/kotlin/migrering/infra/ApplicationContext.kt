package no.nav.etterlatte.migrering

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleProperties
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.ktor.httpClient
import no.nav.etterlatte.migrering.pen.PenKlient

internal class ApplicationContext {
    val dataSource = DataSourceBuilder.createDataSource(System.getenv())

    private val config = ConfigFactory.load()
    val penklient = PenKlient(config, httpClient())

    val featureToggleService: FeatureToggleService =
        FeatureToggleService.initialiser(featureToggleProperties(ConfigFactory.load()))
    val pesysRepository = PesysRepository(dataSource)
}

private fun featureToggleProperties(config: Config) =
    FeatureToggleProperties(
        applicationName = config.getString("funksjonsbrytere.unleash.applicationName"),
        host = config.getString("funksjonsbrytere.unleash.host"),
        apiKey = config.getString("funksjonsbrytere.unleash.token"),
    )
