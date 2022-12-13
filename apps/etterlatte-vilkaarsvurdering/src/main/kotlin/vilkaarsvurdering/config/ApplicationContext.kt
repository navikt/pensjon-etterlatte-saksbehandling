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
import no.nav.etterlatte.vilkaarsvurdering.VilkaarsvurderingRepositoryImpl
import no.nav.etterlatte.vilkaarsvurdering.VilkaarsvurderingService
import no.nav.etterlatte.vilkaarsvurdering.behandling.BehandlingKlientImpl
import no.nav.etterlatte.vilkaarsvurdering.grunnlag.GrunnlagKlientImpl

class ApplicationContext {
    private val properties: ApplicationProperties = ApplicationProperties.fromEnv(System.getenv())
    private val config: Config = ConfigFactory.load()
    val dataSourceBuilder = DataSourceBuilder(properties.jdbcUrl, properties.dbUsername, properties.dbPassword)
    val vilkaarsvurderingService = VilkaarsvurderingService(
        vilkaarsvurderingRepository = VilkaarsvurderingRepositoryImpl(dataSourceBuilder.dataSource()),
        behandlingKlient = BehandlingKlientImpl(config, httpClient()),
        grunnlagKlient = GrunnlagKlientImpl(config, httpClient())
    )

    private fun httpClient() = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            register(ContentType.Application.Json, JacksonConverter(objectMapper))
        }
        defaultRequest {
            header(X_CORRELATION_ID, getCorrelationId())
        }
    }.also { Runtime.getRuntime().addShutdownHook(Thread { it.close() }) }
}