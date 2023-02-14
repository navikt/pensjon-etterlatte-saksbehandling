package no.nav.etterlatte.vilkaarsvurderingasynk

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import no.nav.etterlatte.security.ktor.clientCredential

class AppBuilder(private val props: Map<String, String>) {

    private val vilkaarsvurdering_app = vilkaarsvurderingHttpClient()

    fun createVilkaarsvurderingService(): VilkaarsvurderingService {
        return VilkaarsvurderingService(vilkaarsvurdering_app, "http://etterlatte-vilkaarsvurdering")
    }

    private fun vilkaarsvurderingHttpClient() = HttpClient(OkHttp) {
        expectSuccess = true
        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            }
        }
        install(Auth) {
            clientCredential {
                config = props.toMutableMap()
                    .apply { put("AZURE_APP_OUTBOUND_SCOPE", requireNotNull(get("VILKAARSVURDERING_AZURE_SCOPE"))) }
            }
        }
    }.also { Runtime.getRuntime().addShutdownHook(Thread { it.close() }) }
}