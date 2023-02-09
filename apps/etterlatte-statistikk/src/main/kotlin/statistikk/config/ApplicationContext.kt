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
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.security.ktor.clientCredential
import no.nav.etterlatte.statistikk.clients.BehandlingClient
import no.nav.etterlatte.statistikk.clients.BehandlingClientImpl
import no.nav.etterlatte.statistikk.clients.BeregningClient
import no.nav.etterlatte.statistikk.clients.BeregningClientImpl
import no.nav.etterlatte.statistikk.database.DataSourceBuilder
import no.nav.etterlatte.statistikk.database.SakRepository
import no.nav.etterlatte.statistikk.database.StoenadRepository
import no.nav.etterlatte.statistikk.domain.StoenadRad
import no.nav.etterlatte.statistikk.river.BehandlinghendelseRiver
import no.nav.etterlatte.statistikk.river.VedtakhendelserRiver
import no.nav.etterlatte.statistikk.service.StatistikkService
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import javax.sql.DataSource

class ApplicationContext {
    private val env = System.getenv()
    val rapidsConnection: RapidsConnection = RapidApplication.create(env.withConsumerGroupId())

    val statistikkService: StatistikkService by lazy {
        StatistikkService(statistikkRepository, sakstatistikkRepository, behandlingClient, beregningClient)
    }

    val behandlinghendelseRiver: BehandlinghendelseRiver by lazy {
        BehandlinghendelseRiver(rapidsConnection, statistikkService)
    }

    val vedtakhendelserRiver: VedtakhendelserRiver by lazy {
        VedtakhendelserRiver(rapidsConnection, statistikkService)
    }

    private val behandlingClient: BehandlingClient by lazy {
        BehandlingClientImpl(behandlingHttpClient)
    }

    private val statistikkRepository: StoenadRepository by lazy {
        StoenadRad(datasource)
    }

    private val sakstatistikkRepository: SakRepository by lazy {
        SakRepository(datasource)
    }
    private val datasource: DataSource by lazy { DataSourceBuilder.createDataSource(env).also { it.migrate() } }

    private val beregningClient: BeregningClient by lazy {
        BeregningClientImpl(beregningHttpClient)
    }

    private val beregningHttpClient: HttpClient by lazy {
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
                        .apply { put("AZURE_APP_OUTBOUND_SCOPE", requireNotNull(get("BEREGNING_AZURE_SCOPE"))) }
                }
            }
            defaultRequest {
                header(X_CORRELATION_ID, getCorrelationId())
                url("http://etterlatte-beregning/")
            }
        }.also { Runtime.getRuntime().addShutdownHook(Thread { it.close() }) }
    }

    private val behandlingHttpClient: HttpClient by lazy {
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