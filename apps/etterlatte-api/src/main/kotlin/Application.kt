package no.nav.etterlatte

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.defaultRequest
import io.ktor.http.takeFrom
import no.nav.etterlatte.behandling.BehandlingKlient
import no.nav.etterlatte.behandling.BehandlingService


class ApplicationContext(configLocation: String? = null) {
    private val closables = mutableListOf<() -> Unit>()
    private val config: Config = configLocation?.let { ConfigFactory.load(it) } ?: ConfigFactory.load()

    val behandlingService: BehandlingService

    init {
        behandlingService = endpoint(config.getConfig("no.nav.etterlatte.tjenester.behandling")).also {
            closables.add(it::close)
        }.let {
            BehandlingService(BehandlingKlient(it))
        }
    }

    private fun endpoint(endpointConfig: Config) = HttpClient(CIO) {
        defaultRequest {
            url.takeFrom(endpointConfig.getString("url") + url.encodedPath)
        }
    }

}


fun main() {
    ApplicationContext()
        .also { Server(it).run() }
}
