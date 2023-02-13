package no.nav.etterlatte

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import no.nav.etterlatte.ktortokenexchange.SecurityContextMediator
import no.nav.etterlatte.ktortokenexchange.SecurityContextMediatorFactory
import no.nav.etterlatte.libs.ktor.httpClient
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials
import no.nav.etterlatte.pdl.ParallelleSannheterKlient
import no.nav.etterlatte.pdl.PdlKlient
import no.nav.etterlatte.person.PersonService

class ApplicationContext(configLocation: String? = null) {
    private val config: Config = configLocation?.let { ConfigFactory.load(it) } ?: ConfigFactory.load()

    val securityMediator: SecurityContextMediator = SecurityContextMediatorFactory.from(config)
    val personService: PersonService = PersonService(
        pdlKlient = PdlKlient(
            httpClient = pdlhttpclient(config.getConfig("no.nav.etterlatte.tjenester.pdl.aad")),
            apiUrl = config.getString("no.nav.etterlatte.tjenester.pdl.aad.url")
        ),
        ppsKlient = ParallelleSannheterKlient(
            httpClient = httpClient(),
            apiUrl = config.getString("no.nav.etterlatte.tjenester.pps.url")
        )
    )

    private fun pdlhttpclient(aad: Config) = httpClientClientCredentials(
        azureAppClientId = aad.getString("client_id"),
        azureAppJwk = aad.getString("client_jwk"),
        azureAppWellKnownUrl = aad.getString("well_known_url"),
        azureAppScope = aad.getString("outbound")
    )
}
fun main() {
    ApplicationContext()
        .also { Server(it).run() }
}