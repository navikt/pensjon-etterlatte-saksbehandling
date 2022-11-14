package no.nav.etterlatte.vilkaarsvurdering.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.serialization.jackson.JacksonConverter
import no.nav.etterlatte.libs.common.logging.X_CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.restModule
import no.nav.etterlatte.vilkaarsvurdering.VilkaarsvurderingRepositoryImpl
import no.nav.etterlatte.vilkaarsvurdering.VilkaarsvurderingService
import no.nav.etterlatte.vilkaarsvurdering.behandling.BehandlingKlientImpl
import no.nav.etterlatte.vilkaarsvurdering.grunnlag.GrunnlagKlientImpl
import no.nav.helse.rapids_rivers.RapidApplication
import java.util.*

class ApplicationBuilder {
    private val env = System.getenv()
    private val properties: ApplicationProperties = ApplicationProperties.fromEnv(env)
    private val config: Config = ConfigFactory.load()

    private val dataSourceBuilder = DataSourceBuilder(
        jdbcUrl = properties.jdbcUrl,
        username = properties.dbUsername,
        password = properties.dbPassword
    ).apply { migrate() }

    private val dataSource = dataSourceBuilder.dataSource()
    private val vilkaarsvurderingRepository = VilkaarsvurderingRepositoryImpl(dataSource)
    private val behandlingKlient = BehandlingKlientImpl(config, httpClient())
    private val grunnlagKlient = GrunnlagKlientImpl(config, httpClient())
    private val vilkaarsvurderingService =
        VilkaarsvurderingService(vilkaarsvurderingRepository, ::publiser)

    private val rapidsConnection =
        RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(env.withConsumerGroupId()))
            .withKtorModule {
                restModule(
                    vilkaarsvurderingService = vilkaarsvurderingService,
                    behandlingKlient = behandlingKlient,
                    grunnlagKlient = grunnlagKlient,
                    vilkaarsvurderingRepository = vilkaarsvurderingRepository
                )
            }
            .build()

    fun start() = rapidsConnection.start()
    private fun publiser(melding: String, key: UUID) {
        rapidsConnection.publish(message = melding, key = key.toString())
    }
}

private fun httpClient() = HttpClient(OkHttp) {
    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(objectMapper))
    }
    defaultRequest {
        header(X_CORRELATION_ID, getCorrelationId())
    }
}.also { Runtime.getRuntime().addShutdownHook(Thread { it.close() }) }

fun Map<String, String>.withConsumerGroupId() =
    this.toMutableMap().apply {
        put("KAFKA_CONSUMER_GROUP_ID", get("NAIS_APP_NAME")!!.replace("-", ""))
    }