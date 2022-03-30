package no.nav.etterlatte

import com.typesafe.config.ConfigFactory
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.features.auth.Auth
import io.ktor.client.features.defaultRequest
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.http.takeFrom
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.opplysninger.kilde.inntektskomponenten.InntektsKomponentenService
import no.nav.etterlatte.opplysninger.kilde.inntektskomponenten.OpplysningsByggerService
import no.nav.etterlatte.security.ktor.clientCredential

class AppBuilder(private val props: Map<String, String>) {

    private val config = ConfigFactory.load()

    fun createInntektsKomponentService(): InntektsKomponenten {
        return InntektsKomponentenService(
            inntektsKomponentClient(),
            config.getString("no.nav.etterlatte.tjenester.inntektskomponenten")
        )
    }

    fun createOpplysningsbygger(): OpplysningsBygger {
        return OpplysningsByggerService()
    }

    private fun inntektsKomponentClient() = HttpClient(OkHttp) {
        val env = mutableMapOf(
            "AZURE_APP_CLIENT_ID" to config.getString("client_id"),
            "AZURE_APP_WELL_KNOWN_URL" to config.getString("well_known_url"),
            "AZURE_APP_OUTBOUND_SCOPE" to config.getString("outbound"),
            "AZURE_APP_JWK" to config.getString("client_jwk")
        )
        install(JsonFeature) { serializer = JacksonSerializer(objectMapper) }
        install(Auth) {
            clientCredential {
                config = env
            }
        }
        defaultRequest {
            url.takeFrom(config.getString("url") + url.encodedPath)
        }
    }.also { Runtime.getRuntime().addShutdownHook(Thread { it.close() }) }

}

