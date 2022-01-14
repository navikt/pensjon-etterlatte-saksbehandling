package no.nav.etterlatte


import com.fasterxml.jackson.databind.JsonNode
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.application.ApplicationCall
import io.ktor.auth.Authentication
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.auth.Auth
import io.ktor.client.features.auth.providers.bearer
import io.ktor.client.features.defaultRequest
import io.ktor.http.takeFrom
import com.github.michaelbull.result.mapBoth
import io.ktor.auth.authentication
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.parseAuthorizationHeader
import io.ktor.http.HttpStatusCode
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.request.uri
import io.ktor.response.respond
import no.nav.etterlatte.behandling.BehandlingKlient
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.Configuration
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import no.nav.etterlatte.libs.ktorobo.ThrowableErrorMessage

class ApplicationContext(configLocation: String? = null) {
    private val closables = mutableListOf<() -> Unit>()
    //private val config: Config = configLocation?.let { ConfigFactory.load(it) } ?: ConfigFactory.load()

    val behandlingService: BehandlingService


    init {

        behandlingService = BehandlingService(BehandlingKlient())
        /*
        behandlingService = endpoint(config.getConfig("no.nav.etterlatte.tjenester.behandling")).also {
            closables.add(it::close)
        }.let {
            BehandlingService(BehandlingKlient(it))
        }

         */



    }


    /*
        På vegne av innlogget sluttbruker
     */
    private fun endpoint(endpointConfig: Config) = HttpClient(CIO) {

        install(Auth) {
            bearer {

            }
        }

        defaultRequest {
            url.takeFrom(endpointConfig.getString("url") + url.encodedPath)
        }
    }

}


fun main() {
    ApplicationContext()
        .also { Server(it).run() }
}
