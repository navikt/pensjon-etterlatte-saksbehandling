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
import no.nav.etterlatte.fordeler.FordelerRepository
import no.nav.etterlatte.fordeler.FordelerService
import no.nav.etterlatte.libs.common.logging.X_CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.pdltjenester.PdlTjenesterKlient
import no.nav.etterlatte.security.ktor.clientCredential
import no.nav.helse.rapids_rivers.RapidApplication
import org.slf4j.LoggerFactory

val sikkerLogg = LoggerFactory.getLogger("sikkerLogg")

fun main() {
    sikkerLogg.info("SikkerLogg: etterlatte-fordeler oppstart")

    val env = System.getenv().toMutableMap().apply {
        put("KAFKA_CONSUMER_GROUP_ID", requireNotNull(get("NAIS_APP_NAME")).replace("-", ""))
    }
    val dataSource = DataSourceBuilder.createDataSource(env).apply { migrate() }

    val appBuilder = AppBuilder(env)
    RapidApplication.create(env)
        .also {
            Fordeler(
                rapidsConnection = it,
                fordelerService = FordelerService(
                    FordelerKriterier(),
                    appBuilder.pdlTjenesterKlient(),
                    FordelerRepository(dataSource),
                    maxFordelingTilDoffen = env.longFeature("FEATURE_MAX_FORDELING_TIL_DOFFEN")
                )
            )
        }.start()
}

fun Map<String, String>.longFeature(featureName: String, default: Long = 0): Long {
    return (this[featureName]?.toLong() ?: default).takeIf { it > -1 } ?: Long.MAX_VALUE
}

class AppBuilder(val env: Map<String, String>) {

    internal fun pdlTjenesterKlient() = PdlTjenesterKlient(
        client = pdlTjenesterHttpClient(),
        apiUrl = requireNotNull(env["PDL_URL"])
    )

    private fun pdlTjenesterHttpClient() = HttpClient(OkHttp) {
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
}