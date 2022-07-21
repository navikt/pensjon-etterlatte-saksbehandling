package no.nav.etterlatte


import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import no.nav.etterlatte.fordeler.Fordeler
import no.nav.etterlatte.fordeler.FordelerKriterier
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

    RapidApplication.create(env) { _, kafkaRapid ->
        kafkaRapid.seekToBeginning()}
            .also {
            Fordeler(
                rapidsConnection = it, fordelerService = FordelerService(FordelerKriterier(), pdlTjenesterKlient(env))
            )
        }.start()
}

private fun pdlTjenesterKlient(env: MutableMap<String, String>) = PdlTjenesterKlient(
    client = pdlTjenesterHttpClient(env), apiUrl = requireNotNull(env["PDL_URL"])
)

private fun pdlTjenesterHttpClient(env: MutableMap<String, String>) = HttpClient(OkHttp) {
    expectSuccess = true
    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(objectMapper))
    }
    install(Auth) {
        clientCredential {
            config =
                env.toMutableMap().apply { put("AZURE_APP_OUTBOUND_SCOPE", requireNotNull(get("PDL_AZURE_SCOPE"))) }
        }
    }
    defaultRequest {
        header(X_CORRELATION_ID, getCorrelationId())
    }
}.also {
    Runtime.getRuntime().addShutdownHook(Thread { it.close() })
}


