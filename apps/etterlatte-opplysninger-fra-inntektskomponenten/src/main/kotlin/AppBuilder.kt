package no.nav.etterlatte

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.features.auth.Auth
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.security.ktor.clientCredential

class AppBuilder(private val props: Map<String, String>) {

    fun createInntektsKomponentService(): InntektsKomponenten {
        return InntektsKomponentenService(inntektsKomponentClient(), "https://app-q2.adeo.no")
    }

    private fun inntektsKomponentClient() = HttpClient(OkHttp) {
        install(JsonFeature) { serializer = JacksonSerializer{
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        } }
        install(Auth) {
            clientCredential {
                config = props.toMutableMap()
                    .apply { put("AZURE_APP_OUTBOUND_SCOPE", requireNotNull(get("BEHANDLING_AZURE_SCOPE"))) }
            }
        }
    }.also { Runtime.getRuntime().addShutdownHook(Thread { it.close() }) }

}

interface InntektsKomponenten {
    fun hentInntektListe(fnr: Foedselsnummer)
}