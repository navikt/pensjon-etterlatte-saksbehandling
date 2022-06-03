package no.nav.etterlatte

import com.typesafe.config.ConfigFactory
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.features.auth.Auth
import io.ktor.client.features.defaultRequest
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.header
import io.ktor.http.takeFrom
import no.nav.etterlatte.libs.common.logging.X_CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.opplysninger.kilde.inntektskomponenten.InntektsKomponentenService
import no.nav.etterlatte.opplysninger.kilde.inntektskomponenten.OpplysningsByggerService
import no.nav.etterlatte.security.ktor.clientCredential

class AppBuilder(private val props: Map<String, String>) {

    private val config = ConfigFactory.load()

    fun createInntektsKomponentService(): InntektsKomponenten {
        return InntektsKomponentenService(
            inntektsKomponentClient(),
            config.getString("no.nav.etterlatte.tjenester.inntektskomponenten.proxyUrl")
        )
    }

    fun createOpplysningsbygger(): OpplysningsBygger {
        return OpplysningsByggerService()
    }

    private fun inntektsKomponentClient() = HttpClient(OkHttp) {
        val inntektsConfig = config.getConfig("no.nav.etterlatte.tjenester.inntektskomponenten")
        val env = mutableMapOf(
            "AZURE_APP_CLIENT_ID" to inntektsConfig.getString("clientId"),
            "AZURE_APP_WELL_KNOWN_URL" to inntektsConfig.getString("wellKnownUrl"),
            "AZURE_APP_OUTBOUND_SCOPE" to inntektsConfig.getString("outbound"),
            "AZURE_APP_JWK" to inntektsConfig.getString("clientJwk")
        )
        install(JsonFeature) { serializer = JacksonSerializer(objectMapper) }
        install(Auth) {
            clientCredential {
                config = env
            }
        }
        defaultRequest {
            header(X_CORRELATION_ID, getCorrelationId())
        }
    }.also { Runtime.getRuntime().addShutdownHook(Thread { it.close() }) }

}

