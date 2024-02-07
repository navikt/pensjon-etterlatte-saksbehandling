package no.nav.etterlatte.vedtaksvurdering.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.server.config.HoconApplicationConfig
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleProperties
import no.nav.etterlatte.jobs.MetrikkerJob
import no.nav.etterlatte.jobs.addShutdownHook
import no.nav.etterlatte.libs.common.logging.sikkerLoggOppstartOgAvslutning
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.jobs.LeaderElection
import no.nav.etterlatte.libs.ktor.httpClient
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.etterlatte.libs.ktor.setReady
import no.nav.etterlatte.no.nav.etterlatte.vedtaksvurdering.metrics.VedtakMetrics
import no.nav.etterlatte.no.nav.etterlatte.vedtaksvurdering.metrics.VedtakMetrikkerDao
import no.nav.etterlatte.vedtaksvurdering.AutomatiskBehandlingService
import no.nav.etterlatte.vedtaksvurdering.VedtakBehandlingService
import no.nav.etterlatte.vedtaksvurdering.VedtakSamordningService
import no.nav.etterlatte.vedtaksvurdering.VedtakTilbakekrevingService
import no.nav.etterlatte.vedtaksvurdering.VedtaksvurderingRapidService
import no.nav.etterlatte.vedtaksvurdering.VedtaksvurderingRepository
import no.nav.etterlatte.vedtaksvurdering.VedtaksvurderingService
import no.nav.etterlatte.vedtaksvurdering.automatiskBehandlingRoutes
import no.nav.etterlatte.vedtaksvurdering.klienter.BehandlingKlientImpl
import no.nav.etterlatte.vedtaksvurdering.klienter.BeregningKlientImpl
import no.nav.etterlatte.vedtaksvurdering.klienter.SamKlientImpl
import no.nav.etterlatte.vedtaksvurdering.klienter.TrygdetidKlient
import no.nav.etterlatte.vedtaksvurdering.klienter.VilkaarsvurderingKlientImpl
import no.nav.etterlatte.vedtaksvurdering.samordningsvedtakRoute
import no.nav.etterlatte.vedtaksvurdering.tilbakekrevingvedtakRoute
import no.nav.etterlatte.vedtaksvurdering.vedtaksvurderingRoute
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.Logger
import rapidsandrivers.getRapidEnv
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.UUID

val sikkerLogg: Logger = sikkerlogger()

class ApplicationBuilder {
    init {
        sikkerLoggOppstartOgAvslutning("etterlatte-vedtaksvurdering")
    }

    private val env = System.getenv()
    private val config: Config = ConfigFactory.load()
    private val dataSource = DataSourceBuilder.createDataSource(env)

    private val behandlingKlient = BehandlingKlientImpl(config, httpClient())
    private val samKlient =
        SamKlientImpl(
            config,
            httpClientClientCredentials(
                azureAppClientId = config.getString("azure.app.client.id"),
                azureAppJwk = config.getString("azure.app.jwk"),
                azureAppWellKnownUrl = config.getString("azure.app.well.known.url"),
                azureAppScope = config.getString("samordnevedtak.azure.scope"),
            ),
        )
    private val trygdetidKlient = TrygdetidKlient(config, httpClient())
    private val vedtaksvurderingService =
        VedtaksvurderingService(repository = VedtaksvurderingRepository.using(dataSource))
    private val vedtakBehandlingService =
        VedtakBehandlingService(
            repository = VedtaksvurderingRepository.using(dataSource),
            beregningKlient = BeregningKlientImpl(config, httpClient()),
            vilkaarsvurderingKlient = VilkaarsvurderingKlientImpl(config, httpClient()),
            behandlingKlient = behandlingKlient,
            samKlient = samKlient,
            trygdetidKlient = trygdetidKlient,
        )
    private val vedtaksvurderingRapidService = VedtaksvurderingRapidService(publiser = ::publiser)
    private val vedtakTilbakekrevingService =
        VedtakTilbakekrevingService(
            repository = VedtaksvurderingRepository(dataSource),
        )
    private val vedtakSamordningService =
        VedtakSamordningService(
            repository = VedtaksvurderingRepository(dataSource),
        )
    private val automatiskBehandlingService =
        AutomatiskBehandlingService(
            vedtakBehandlingService,
            behandlingKlient,
        )

    val leaderElectionKlient = LeaderElection(env["ELECTOR_PATH"], httpClient())
    private val metrikkerJob: MetrikkerJob by lazy {
        MetrikkerJob(
            VedtakMetrics(VedtakMetrikkerDao.using(dataSource)),
            leaderElectionKlient,
            Duration.of(10, ChronoUnit.MINUTES).toMillis(),
            periode = Duration.of(5, ChronoUnit.MINUTES),
        )
    }

    private val rapidsConnection =
        RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(getRapidEnv()))
            .withKtorModule {
                restModule(sikkerLogg, config = HoconApplicationConfig(config)) {
                    vedtaksvurderingRoute(vedtaksvurderingService, vedtakBehandlingService, vedtaksvurderingRapidService, behandlingKlient)
                    automatiskBehandlingRoutes(automatiskBehandlingService, behandlingKlient)
                    samordningsvedtakRoute(vedtakSamordningService)
                    tilbakekrevingvedtakRoute(vedtakTilbakekrevingService, behandlingKlient)
                    metrikkerJob.schedule().also { addShutdownHook(it) }
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

    private fun featureToggleProperties(config: Config) =
        FeatureToggleProperties(
            applicationName = config.getString("funksjonsbrytere.unleash.applicationName"),
            host = config.getString("funksjonsbrytere.unleash.host"),
            apiKey = config.getString("funksjonsbrytere.unleash.token"),
        )
}
