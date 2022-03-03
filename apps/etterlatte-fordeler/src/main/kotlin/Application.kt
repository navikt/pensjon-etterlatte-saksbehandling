package no.nav.etterlatte


import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.features.auth.Auth
import io.ktor.client.features.defaultRequest
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.header
import no.nav.etterlatte.fordeler.Fordeler
import no.nav.etterlatte.fordeler.FordelerKriterierService
import no.nav.etterlatte.fordeler.FordelerService
import no.nav.etterlatte.libs.common.logging.X_CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.pdltjenester.PdlTjenesterKlient
import no.nav.etterlatte.security.ktor.clientCredential
import no.nav.helse.rapids_rivers.RapidApplication

fun main() {
    val env = System.getenv().toMutableMap().apply {
        put("KAFKA_CONSUMER_GROUP_ID", requireNotNull(get("NAIS_APP_NAME")).replace("-", ""))
    }

    RapidApplication.create(env)
        .also {
            Fordeler(
                rapidsConnection = it,
                fordelerService = FordelerService(FordelerKriterierService(), pdlTjenesterKlient(env))
            )
        }.start()
}

private fun pdlTjenesterKlient(env: MutableMap<String, String>) =
    PdlTjenesterKlient(
        client = pdlTjenesterHttpClient(env),
        apiUrl = requireNotNull(env["PDL_URL"])
    )

private fun pdlTjenesterHttpClient(env: MutableMap<String, String>) =
    HttpClient(OkHttp) {
        install(JsonFeature) { serializer = JacksonSerializer(objectMapper) }
        install(Auth) {
            clientCredential {
                config = env.toMutableMap()
                    .apply { put("AZURE_APP_OUTBOUND_SCOPE", requireNotNull(get("PDL_AZURE_SCOPE"))) }
            }
        }
        defaultRequest {
            header(X_CORRELATION_ID, getCorrelationId())
        }
    }.also { Runtime.getRuntime().addShutdownHook(Thread { it.close() }) }


