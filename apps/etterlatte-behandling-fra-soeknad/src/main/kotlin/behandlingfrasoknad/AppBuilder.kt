package no.nav.etterlatte.behandlingfrasoknad

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.features.auth.Auth
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import no.nav.etterlatte.security.ktor.clientCredential

class AppBuilder(private val props: Map<String, String>) {

    private val behandling_app = behandlingHttpClient()


    fun createBehandlingService(): Behandling {
        return BehandlingsService(behandling_app, "http://etterlatte-behandling")
    }

    private fun behandlingHttpClient() = HttpClient(OkHttp) {
        install(JsonFeature) { serializer = JacksonSerializer{registerModule(JavaTimeModule())} }
        install(Auth) {
            clientCredential {
                config = props.toMutableMap()
                    .apply { put("AZURE_APP_OUTBOUND_SCOPE", requireNotNull(get("BEHANDLING_AZURE_SCOPE"))) }
            }
        }
    }.also { Runtime.getRuntime().addShutdownHook(Thread { it.close() }) }


}


