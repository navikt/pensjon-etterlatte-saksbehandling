package no.nav.etterlatte

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.features.auth.Auth
import io.ktor.client.features.defaultRequest
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.header
import no.nav.etterlatte.libs.common.logging.X_CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.pdltjenester.PdlTjenesterKlient
import no.nav.etterlatte.security.ktor.clientCredential

class AppBuilder(private val props: Map<String, String>) {
    companion object {
        const val CONFIG_PDL_URL = "PDL_URL"
    }

    fun createPdlTjenesterKlient(): PdlTjenesterKlient {
        return PdlTjenesterKlient(pdlTjenesterHttpClient(), props[CONFIG_PDL_URL]!!)
    }

    private fun pdlTjenesterHttpClient() = HttpClient(OkHttp) {
        defaultRequest {
            header(X_CORRELATION_ID, getCorrelationId())
        }
        install(JsonFeature) { serializer = JacksonSerializer() }
        install(Auth) {
            clientCredential {
                config = props.toMutableMap()
                    .apply { put("AZURE_APP_OUTBOUND_SCOPE", requireNotNull(get("PDL_AZURE_SCOPE"))) }
            }
        }
    }.also { Runtime.getRuntime().addShutdownHook(Thread { it.close() }) }

}