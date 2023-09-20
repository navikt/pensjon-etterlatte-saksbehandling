package no.nav.etterlatte.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import no.nav.etterlatte.libs.ktor.httpClient
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials
import no.nav.etterlatte.pdl.ParallelleSannheterKlient
import no.nav.etterlatte.pdl.PdlKlient
import no.nav.etterlatte.person.PersonService

class ApplicationContext(env: Map<String, String>) {
    val config: Config = ConfigFactory.load()
    val httpPort = env.getOrDefault("HTTP_PORT", "8080").toInt()

    val pdlKlient =
        PdlKlient(
            httpClient =
                httpClientClientCredentials(
                    azureAppClientId = config.getString("pdl.client_id"),
                    azureAppJwk = config.getString("pdl.client_jwk"),
                    azureAppWellKnownUrl = config.getString("pdl.well_known_url"),
                    azureAppScope = config.getString("pdl.outbound"),
                ),
            apiUrl = config.getString("pdl.url"),
        )

    val parallelleSannheterKlient =
        ParallelleSannheterKlient(
            httpClient = httpClient(),
            apiUrl = config.getString("pps.url"),
        )

    val personService: PersonService =
        PersonService(
            pdlKlient = pdlKlient,
            ppsKlient = parallelleSannheterKlient,
        )
}
