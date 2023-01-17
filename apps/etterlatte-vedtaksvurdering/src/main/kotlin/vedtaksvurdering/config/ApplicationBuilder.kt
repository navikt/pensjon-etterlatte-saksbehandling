package no.nav.etterlatte.vedtaksvurdering.config

import com.fasterxml.jackson.core.type.TypeReference
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.ktor.httpClient
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.etterlatte.vedtaksvurdering.VedtaksvurderingRepository
import no.nav.etterlatte.vedtaksvurdering.VedtaksvurderingService
import no.nav.etterlatte.vedtaksvurdering.klienter.BehandlingKlientImpl
import no.nav.etterlatte.vedtaksvurdering.klienter.BeregningKlientImpl
import no.nav.etterlatte.vedtaksvurdering.klienter.VilkaarsvurderingKlientImpl
import no.nav.etterlatte.vedtaksvurdering.rivers.LagreIverksattVedtak
import no.nav.etterlatte.vedtaksvurdering.vedtaksvurderingRoute
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import java.util.*

class ApplicationBuilder {
    private val env = System.getenv()
    private val config: Config = ConfigFactory.load()
    private val properties: ApplicationProperties = ApplicationProperties.fromEnv(env)
    private val dataSource = DataSourceBuilder.createDataSource(
        jdbcUrl = properties.jdbcUrl,
        username = properties.dbUsername,
        password = properties.dbPassword
    )
    private fun getSaksbehandlere(): Map<String, String> {
        val saksbehandlereSecret = env["saksbehandlere"]!!
        return objectMapper.readValue(
            saksbehandlereSecret,
            object : TypeReference<Map<String, String>>() {}
        )
    }
    private val vedtaksvurderingService = VedtaksvurderingService(
        repository = VedtaksvurderingRepository.using(dataSource),
        beregningKlient = BeregningKlientImpl(config, httpClient()),
        vilkaarsvurderingKlient = VilkaarsvurderingKlientImpl(config, httpClient()),
        behandlingKlient = BehandlingKlientImpl(config, httpClient()),
        sendToRapid = ::publiser,
        saksbehandlere = getSaksbehandlere()
    )

    private val rapidsConnection =
        RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(env.withConsumerGroupId()))
            .withKtorModule {
                restModule {
                    vedtaksvurderingRoute(vedtaksvurderingService)
                }
            }
            .build()
            .apply {
                register(object : RapidsConnection.StatusListener {
                    override fun onStartup(rapidsConnection: RapidsConnection) {
                        dataSource.migrate()
                    }
                })
                LagreIverksattVedtak(
                    rapidsConnection = this,
                    vedtaksvurderingService = vedtaksvurderingService,
                    behandlingHttpClient = httpClientClientCredentials(
                        azureAppClientId = config.getString("azure.app.client.id"),
                        azureAppJwk = config.getString("azure.app.client.secret"),
                        azureAppWellKnownUrl = config.getString("azure.app.well.known.url"),
                        azureAppScope = config.getString("behandling.azure.scope")
                    )
                )
            }

    fun start() = rapidsConnection.start()

    fun publiser(melding: String, key: UUID) { rapidsConnection.publish(message = melding, key = key.toString()) }
}

private fun Map<String, String>.withConsumerGroupId() =
    this.toMutableMap().apply {
        put("KAFKA_CONSUMER_GROUP_ID", get("NAIS_APP_NAME")!!.replace("-", ""))
    }