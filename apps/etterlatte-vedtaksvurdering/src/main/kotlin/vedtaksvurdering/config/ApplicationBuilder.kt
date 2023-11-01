package no.nav.etterlatte.vedtaksvurdering.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.server.config.HoconApplicationConfig
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleProperties
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.logging.sikkerLoggOppstartOgAvslutning
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.ktor.httpClient
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.etterlatte.libs.ktor.setReady
import no.nav.etterlatte.vedtaksvurdering.VedtakBehandlingService
import no.nav.etterlatte.vedtaksvurdering.VedtakTilbakekrevingService
import no.nav.etterlatte.vedtaksvurdering.VedtaksvurderingRepository
import no.nav.etterlatte.vedtaksvurdering.VedtaksvurderingService
import no.nav.etterlatte.vedtaksvurdering.automatiskBehandlingRoutes
import no.nav.etterlatte.vedtaksvurdering.klienter.BehandlingKlientImpl
import no.nav.etterlatte.vedtaksvurdering.klienter.BeregningKlientImpl
import no.nav.etterlatte.vedtaksvurdering.klienter.SamKlientImpl
import no.nav.etterlatte.vedtaksvurdering.klienter.VilkaarsvurderingKlientImpl
import no.nav.etterlatte.vedtaksvurdering.samordningsvedtakRoute
import no.nav.etterlatte.vedtaksvurdering.tilbakekrevingvedtakRoute
import no.nav.etterlatte.vedtaksvurdering.vedtaksvurderingRoute
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.Logger
import rapidsandrivers.getRapidEnv
import java.util.UUID

val sikkerLogg: Logger = sikkerlogger()

class ApplicationBuilder {
    init {
        sikkerLoggOppstartOgAvslutning("etterlatte-vedtaksvurdering")
    }

    private val env = System.getenv()
    private val config: Config = ConfigFactory.load()
    private val properties: ApplicationProperties = ApplicationProperties.fromEnv(env)
    private val dataSource =
        DataSourceBuilder.createDataSource(
            jdbcUrl = properties.jdbcUrl,
            username = properties.dbUsername,
            password = properties.dbPassword,
        )

    private val featureToggleService = FeatureToggleService.initialiser(featureToggleProperties(config))
    private val behandlingKlient = BehandlingKlientImpl(config, httpClient())
    private val samKlient =
        SamKlientImpl(
            config,
            httpClientClientCredentials(
                azureAppClientId = config.getString("azure.app.client.id"),
                azureAppJwk = config.getString("azure.app.jwk"),
                azureAppWellKnownUrl = config.getString("azure.app.well.known.url"),
                azureAppScope = config.getString("azure.app.well.known.url"),
            ),
            featureToggleService,
        )
    private val vedtaksvurderingService =
        VedtaksvurderingService(repository = VedtaksvurderingRepository.using(dataSource))
    private val vedtakBehandlingService =
        VedtakBehandlingService(
            repository = VedtaksvurderingRepository.using(dataSource),
            beregningKlient = BeregningKlientImpl(config, httpClient()),
            vilkaarsvurderingKlient = VilkaarsvurderingKlientImpl(config, httpClient()),
            behandlingKlient = behandlingKlient,
            samKlient = samKlient,
            publiser = ::publiser,
        )
    private val vedtakTilbakekrevingService =
        VedtakTilbakekrevingService(
            repository = VedtaksvurderingRepository(dataSource),
        )

    private val rapidsConnection =
        RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(getRapidEnv()))
            .withKtorModule {
                restModule(sikkerLogg, config = HoconApplicationConfig(config)) {
                    vedtaksvurderingRoute(vedtaksvurderingService, vedtakBehandlingService, behandlingKlient)
                    automatiskBehandlingRoutes(vedtakBehandlingService, behandlingKlient)
                    samordningsvedtakRoute(vedtaksvurderingService, vedtakBehandlingService)
                    tilbakekrevingvedtakRoute(vedtakTilbakekrevingService, behandlingKlient)
                }
            }
            .build()
            .apply {
                register(
                    object : RapidsConnection.StatusListener {
                        override fun onStartup(rapidsConnection: RapidsConnection) {
                            dataSource.migrate()
                        }
                    },
                )
            }

    fun start() = setReady().also { rapidsConnection.start() }

    private fun publiser(
        melding: String,
        key: UUID,
    ) {
        rapidsConnection.publish(message = melding, key = key.toString())
    }
}

private fun featureToggleProperties(config: Config) =
    FeatureToggleProperties(
        applicationName = config.getString("funksjonsbrytere.unleash.applicationName"),
        host = config.getString("funksjonsbrytere.unleash.host"),
        apiKey = config.getString("funksjonsbrytere.unleash.token"),
    )
