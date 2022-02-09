package no.nav.etterlatte.opplysninger.kilde.pdl

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.features.auth.Auth
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import no.nav.etterlatte.security.ktor.clientCredential

class AppBuilder(private val props: Map<String, String>) {

    private val behandling_app = behandlingHttpClient()
    private val pdlTjenester = pdlTjenesterHttpClient()


    fun createBehandlingService(): Behandling {
        return BehandlingsService(behandling_app, "http://etterlatte-behandling")
    }

    fun createPdlService(): Pdl {
        return PdlService(pdlTjenester, "http://etterlatte-pdltjenester")
    }

    fun createOpplysningsbygger(): OpplysningsBygger {
        return OpplysningsByggerService()
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

    private fun pdlTjenesterHttpClient() = HttpClient(OkHttp) {
        install(JsonFeature) { serializer = JacksonSerializer{registerModule(JavaTimeModule())} }
        install(Auth) {
            clientCredential {
                config = props.toMutableMap()
                    .apply { put("AZURE_APP_OUTBOUND_SCOPE", requireNotNull(get("PDLTJENESTER_AZURE_SCOPE"))) }
            }
        }
    }.also { Runtime.getRuntime().addShutdownHook(Thread { it.close() }) }


}


