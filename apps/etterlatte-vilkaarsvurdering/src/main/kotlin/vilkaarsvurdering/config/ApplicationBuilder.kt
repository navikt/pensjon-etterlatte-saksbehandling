package no.nav.etterlatte.vilkaarsvurdering.config

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.serialization.jackson.JacksonConverter
import no.nav.etterlatte.libs.common.logging.X_CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.restModule
import no.nav.etterlatte.security.ktor.clientCredential
import no.nav.etterlatte.vilkaarsvurdering.VilkaarsvurderingRepositoryImpl
import no.nav.etterlatte.vilkaarsvurdering.VilkaarsvurderingService
import no.nav.etterlatte.vilkaarsvurdering.behandling.BehandlingKlientImpl
import no.nav.helse.rapids_rivers.RapidApplication
import java.util.*

class ApplicationBuilder {
    private val env = System.getenv()
    private val properties: ApplicationProperties = ApplicationProperties.fromEnv(env)

    private val dataSourceBuilder = DataSourceBuilder(
        jdbcUrl = properties.jdbcUrl,
        username = properties.dbUsername,
        password = properties.dbPassword
    ).apply { migrate() }

    private val dataSource = dataSourceBuilder.dataSource()
    private val vilkaarsvurderingRepository = VilkaarsvurderingRepositoryImpl(dataSource)
    private val behandlingKlient =
        BehandlingKlientImpl(requireNotNull(env["ETTERLATTE_BEHANDLING_URL"]), behandlingHttpClient(env))
    private val vilkaarsvurderingService =
        VilkaarsvurderingService(vilkaarsvurderingRepository, ::publiser)

    private val rapidsConnection =
        RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(env.withConsumerGroupId()))
            .withKtorModule {
                restModule(
                    vilkaarsvurderingService = vilkaarsvurderingService,
                    behandlingKlient = behandlingKlient
                )
            }
            .build()

    fun start() = rapidsConnection.start()
    private fun publiser(melding: String, key: UUID) {
        rapidsConnection.publish(message = melding, key = key.toString())
    }
}

private fun behandlingHttpClient(env: MutableMap<String, String>) = HttpClient(OkHttp) {
    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(objectMapper))
    }
    install(Auth) {
        clientCredential {
            config =
                env.toMutableMap()
                    .apply { put("AZURE_APP_OUTBOUND_SCOPE", requireNotNull(get("ETTERLATTE_BEHANDLING_SCOPE"))) }
        }
    }
    defaultRequest {
        header(X_CORRELATION_ID, getCorrelationId())
    }
}.also { Runtime.getRuntime().addShutdownHook(Thread { it.close() }) }

fun Map<String, String>.withConsumerGroupId() =
    this.toMutableMap().apply {
        put("KAFKA_CONSUMER_GROUP_ID", get("NAIS_APP_NAME")!!.replace("-", ""))
    }