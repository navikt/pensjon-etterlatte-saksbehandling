package no.nav.etterlatte.beregningkafka

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.security.ktor.clientCredential
import rapidsandrivers.RapidsAndRiversAppBuilder

class AppBuilder(props: Miljoevariabler) : RapidsAndRiversAppBuilder(props) {

    private val beregningapp = beregningHttpClient()

    fun createBeregningService(): BeregningService {
        return BeregningService(beregningapp, "http://etterlatte-beregning")
    }

    private fun beregningHttpClient() = HttpClient(OkHttp) {
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
                    .apply { put("AZURE_APP_OUTBOUND_SCOPE", requireNotNull(get("BEREGNING_AZURE_SCOPE"))) }
            }
        }
    }.also { Runtime.getRuntime().addShutdownHook(Thread { it.close() }) }
}