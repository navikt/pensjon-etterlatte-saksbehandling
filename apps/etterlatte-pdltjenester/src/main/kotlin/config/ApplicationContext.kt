package no.nav.etterlatte.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import no.nav.etterlatte.EnvKey.HTTP_PORT
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleProperties
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.ktor.httpClient
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials
import no.nav.etterlatte.pdl.ParallelleSannheterKlient
import no.nav.etterlatte.pdl.PdlKlient
import no.nav.etterlatte.pdl.PdlOboKlient
import no.nav.etterlatte.pdl.mapper.ParallelleSannheterService
import no.nav.etterlatte.person.PersonService
import no.nav.etterlatte.personweb.PersonWebService

class ApplicationContext(
    env: Miljoevariabler,
) {
    val config: Config = ConfigFactory.load()
    val httpPort = env.getOrDefault(HTTP_PORT, "8080").toInt()

    private val pdlOboKlient = PdlOboKlient(httpClient(), config)
    private val pdlKlient =
        PdlKlient(
            httpClient =
                httpClientClientCredentials(
                    azureAppClientId = config.getString("azure.app.client.id"),
                    azureAppJwk = config.getString("azure.app.jwk"),
                    azureAppWellKnownUrl = config.getString("azure.app.well.known.url"),
                    azureAppScope = config.getString("pdl.scope"),
                ),
            apiUrl = config.getString("pdl.url"),
        )

    private val featureToggleService = FeatureToggleService.initialiser(featureToggleProperties(config))

    private val parallelleSannheterKlient =
        ParallelleSannheterKlient(
            httpClient = httpClient(),
            apiUrl = config.getString("pps.url"),
            featureToggleService = featureToggleService,
        )

    private val parallelleSannheterService =
        ParallelleSannheterService(
            ppsKlient = parallelleSannheterKlient,
            pdlKlient = pdlKlient,
            pdlOboKlient = pdlOboKlient,
        )

    val personService = PersonService(pdlKlient, parallelleSannheterService)

    val personWebService = PersonWebService(pdlOboKlient, parallelleSannheterService)
}

private fun featureToggleProperties(config: Config) =
    FeatureToggleProperties(
        applicationName = config.getString("funksjonsbrytere.unleash.applicationName"),
        host = config.getString("funksjonsbrytere.unleash.host"),
        apiKey = config.getString("funksjonsbrytere.unleash.token"),
    )
