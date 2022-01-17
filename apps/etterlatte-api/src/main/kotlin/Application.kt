package no.nav.etterlatte


import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.auth.Auth
import io.ktor.client.features.auth.providers.bearer
import io.ktor.client.features.defaultRequest
import io.ktor.http.takeFrom
import no.nav.etterlatte.behandling.BehandlingKlient
import no.nav.etterlatte.behandling.BehandlingService

class ApplicationContext(configLocation: String? = null) {
    private val closables = mutableListOf<() -> Unit>()
    private val config: Config = configLocation?.let { ConfigFactory.load(it) } ?: ConfigFactory.load()

    val behandlingService: BehandlingService

    init {

        behandlingService = BehandlingService(BehandlingKlient(config))
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
