package no.nav.etterlatte.statistikk.config

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
import no.nav.etterlatte.statistikk.clients.BehandlingClient
import no.nav.etterlatte.statistikk.clients.BehandlingClientImpl
import no.nav.etterlatte.statistikk.database.DataSourceBuilder
import no.nav.etterlatte.statistikk.database.SakstatistikkRepository
import no.nav.etterlatte.statistikk.database.StatistikkRepository
import no.nav.etterlatte.statistikk.river.BehandlinghendelseRiver
import no.nav.etterlatte.statistikk.river.VedtakhendelserRiver
import no.nav.etterlatte.statistikk.service.StatistikkService
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection

class ApplicationContext {
    private val env = System.getenv()
    val rapidsConnection: RapidsConnection = RapidApplication.create(env.withConsumerGroupId())

    val statistikkService: StatistikkService by lazy {
        StatistikkService(statistikkRepository, sakstatistikkRepository, behandlingClient)
    }

    val behandlinghendelseRiver: BehandlinghendelseRiver by lazy {
        BehandlinghendelseRiver(rapidsConnection, statistikkService)
    }

    val vedtakhendelserRiver: VedtakhendelserRiver by lazy {
        VedtakhendelserRiver(rapidsConnection, statistikkService)
    }

    private val behandlingClient: BehandlingClient by lazy {
        BehandlingClientImpl(httpClient)
    }

    private val statistikkRepository: StatistikkRepository by lazy {
        StatistikkRepository(datasourceBuilder.dataSource)
    }

    private val sakstatistikkRepository: SakstatistikkRepository by lazy {
        SakstatistikkRepository(datasourceBuilder.dataSource)
    }
    private val datasourceBuilder: DataSourceBuilder by lazy { DataSourceBuilder(env) }

    private val httpClient: HttpClient by lazy {
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
    }
}

private fun Map<String, String>.withConsumerGroupId() =
    this.toMutableMap().apply {
        put("KAFKA_CONSUMER_GROUP_ID", get("NAIS_APP_NAME")!!.replace("-", ""))
    }