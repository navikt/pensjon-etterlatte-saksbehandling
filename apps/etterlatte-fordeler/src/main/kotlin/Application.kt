package no.nav.etterlatte

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.serialization.jackson.JacksonConverter
import no.nav.etterlatte.fordeler.Fordeler
import no.nav.etterlatte.fordeler.FordelerKriterier
import no.nav.etterlatte.fordeler.FordelerService
import no.nav.etterlatte.fordeler.digdirkrr.KontaktinfoKlient
import no.nav.etterlatte.libs.common.logging.X_CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.pdltjenester.PdlTjenesterKlient
import no.nav.etterlatte.security.ktor.clientCredential
import no.nav.helse.rapids_rivers.RapidApplication
import org.slf4j.LoggerFactory

val sikkerLogg = LoggerFactory.getLogger("sikkerLogg")

fun main() {
    val env = System.getenv().toMutableMap().apply {
        put("KAFKA_CONSUMER_GROUP_ID", requireNotNull(get("NAIS_APP_NAME")).replace("-", ""))
    }

    RapidApplication.create(env) { _, kafkaRapid ->
        kafkaRapid.seekToBeginning()
    }
        .also {
            Fordeler(
                rapidsConnection = it,
                fordelerService = FordelerService(FordelerKriterier(digdirkrrKlient(env)), pdlTjenesterKlient(env))
            )
        }.start()
}

private fun pdlTjenesterKlient(env: MutableMap<String, String>) = PdlTjenesterKlient(
    client = ClientCredentialsHttpClient(env, requireNotNull(env.get("PDL_AZURE_SCOPE"))),
    apiUrl = requireNotNull(env["PDL_URL"])
)

private fun digdirkrrKlient(env: MutableMap<String, String>) = KontaktinfoKlient(
    client = ClientCredentialsHttpClient(env, requireNotNull(env.get("DIGDIR_KRR_AZURE_SCOPE"))),
    apiUrl = requireNotNull(env["DIGDIRR_KRR_URL"])
)

private fun ClientCredentialsHttpClient(env: MutableMap<String, String>, azureScope: String) = HttpClient(OkHttp) {
    expectSuccess = true
    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(objectMapper))
    }
    install(Auth) {
        clientCredential {
            config =
                env.toMutableMap().apply { put("AZURE_APP_OUTBOUND_SCOPE", azureScope) }
        }
    }
    defaultRequest {
        header(X_CORRELATION_ID, getCorrelationId())
    }
}.also {
    Runtime.getRuntime().addShutdownHook(Thread { it.close() })
}