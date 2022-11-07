package no.nav.etterlatte

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.serialization.jackson.jackson
import no.nav.etterlatte.libs.common.logging.X_CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.security.ktor.clientCredential
import no.nav.etterlatte.statistikk.clients.BehandlingClientImpl
import no.nav.etterlatte.statistikk.database.DataSourceBuilder
import no.nav.etterlatte.statistikk.database.StatistikkRepository
import no.nav.etterlatte.statistikk.river.StatistikkRiver
import no.nav.etterlatte.statistikk.statistikk.StatistikkService
import no.nav.helse.rapids_rivers.RapidApplication
import rapidsandrivers.vedlikehold.registrerVedlikeholdsriver

fun main() {
    System.getenv()
        .let { env ->
            env.toMutableMap().also {
                it["KAFKA_CONSUMER_GROUP_ID"] = it["NAIS_APP_NAME"]!!.replace("-", "")
            }
        }
        .also { env ->
            val behandlingHttpClient = behandlingHttpClient(env)
            DataSourceBuilder(env).apply {
                migrate()
            }.dataSource
                .also { dataSource ->
                    val statistikkService = StatistikkService(
                        StatistikkRepository.using(dataSource),
                        BehandlingClientImpl(behandlingHttpClient)
                    )
                    RapidApplication.create(env).apply {
                        StatistikkRiver(this, statistikkService)
                        registrerVedlikeholdsriver(statistikkService)
                    }.start()
                }
        }
}

fun behandlingHttpClient(env: Map<String, String>) =
    HttpClient(OkHttp) {
        expectSuccess = true
        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            }
        }
        install(Auth) {
            clientCredential {
                config = env.toMutableMap()
                    .apply { put("AZURE_APP_OUTBOUND_SCOPE", requireNotNull(get("BEHANDLING_AZURE_SCOPE"))) }
            }
        }
        defaultRequest {
            header(X_CORRELATION_ID, getCorrelationId())
            url("http://etterlatte-behandling/")
        }
    }.also { Runtime.getRuntime().addShutdownHook(Thread { it.close() }) }