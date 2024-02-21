package no.nav.etterlatte.migrering

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleProperties
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.ktor.httpClient
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials
import no.nav.etterlatte.migrering.grunnlag.GrunnlagKlient
import no.nav.etterlatte.migrering.grunnlag.Utenlandstilknytningsjekker
import no.nav.etterlatte.migrering.pen.PenKlient
import no.nav.etterlatte.migrering.person.krr.KrrKlient
import no.nav.etterlatte.migrering.start.StartMigreringRepository
import no.nav.etterlatte.migrering.verge.VergeRepository
import no.nav.etterlatte.migrering.verifisering.GjenlevendeForelderPatcher
import no.nav.etterlatte.migrering.verifisering.PdlTjenesterKlient
import no.nav.etterlatte.migrering.verifisering.PersonHenter
import no.nav.etterlatte.migrering.verifisering.Verifiserer

internal class ApplicationContext {
    val dataSource = DataSourceBuilder.createDataSource(System.getenv())

    private val config = ConfigFactory.load()
    val penklient = PenKlient(config, httpClient())

    val featureToggleService: FeatureToggleService =
        FeatureToggleService.initialiser(featureToggleProperties(ConfigFactory.load()))
    val pesysRepository = PesysRepository(dataSource)
    val vergeRepository = VergeRepository(dataSource)

    val pdlTjenesterKlient =
        PdlTjenesterKlient(
            config,
            httpClientClientCredentials(
                azureAppClientId = config.getString("azure.app.client.id"),
                azureAppJwk = config.getString("azure.app.jwk"),
                azureAppWellKnownUrl = config.getString("azure.app.well.known.url"),
                azureAppScope = config.getString("pdl.azure.scope"),
            ),
        )
    val personHenter = PersonHenter(pdlTjenesterKlient)
    val gjenlevendeForelderPatcher = GjenlevendeForelderPatcher(pdlTjenesterKlient = pdlTjenesterKlient, personHenter)
    val grunnlagKlient =
        GrunnlagKlient(
            config,
            httpClientClientCredentials(
                azureAppClientId = config.getString("azure.app.client.id"),
                azureAppJwk = config.getString("azure.app.jwk"),
                azureAppWellKnownUrl = config.getString("azure.app.well.known.url"),
                azureAppScope = config.getString("grunnlag.azure.scope"),
            ),
        )

    val utenlandstilknytningsjekker = Utenlandstilknytningsjekker(grunnlagKlient)
    val verifiserer =
        Verifiserer(
            repository = pesysRepository,
            gjenlevendeForelderPatcher = gjenlevendeForelderPatcher,
            utenlandstilknytningsjekker = utenlandstilknytningsjekker,
            personHenter = personHenter,
            featureToggleService = featureToggleService,
            grunnlagKlient = grunnlagKlient,
            penKlient = penklient,
        )

    val krrKlient =
        KrrKlient(
            client =
                httpClientClientCredentials(
                    azureAppClientId = config.getString("azure.app.client.id"),
                    azureAppJwk = config.getString("azure.app.jwk"),
                    azureAppWellKnownUrl = config.getString("azure.app.well.known.url"),
                    azureAppScope = config.getString("krr.scope"),
                ),
            url = config.getString("krr.url"),
        )

    val startMigreringRepository = StartMigreringRepository(dataSource)
}

private fun featureToggleProperties(config: Config) =
    FeatureToggleProperties(
        applicationName = config.getString("funksjonsbrytere.unleash.applicationName"),
        host = config.getString("funksjonsbrytere.unleash.host"),
        apiKey = config.getString("funksjonsbrytere.unleash.token"),
    )
