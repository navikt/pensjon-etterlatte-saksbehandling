package no.nav.etterlatte.vedtaksvurdering.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.server.config.HoconApplicationConfig
import no.nav.etterlatte.libs.common.logging.sikkerLoggOppstartOgAvslutning
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.ktor.httpClient
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.etterlatte.libs.ktor.setReady
import no.nav.etterlatte.vedtaksvurdering.VedtaksvurderingRepository
import no.nav.etterlatte.vedtaksvurdering.VedtaksvurderingService
import no.nav.etterlatte.vedtaksvurdering.automatiskBehandlingRoutes
import no.nav.etterlatte.vedtaksvurdering.klienter.BehandlingKlientImpl
import no.nav.etterlatte.vedtaksvurdering.klienter.BeregningKlientImpl
import no.nav.etterlatte.vedtaksvurdering.klienter.VilkaarsvurderingKlientImpl
import no.nav.etterlatte.vedtaksvurdering.vedtaksvurderingRoute
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import rapidsandrivers.getRapidEnv
import java.util.*

val sikkerLogg: Logger = LoggerFactory.getLogger("sikkerLogg")

class ApplicationBuilder {
    init {
        sikkerLoggOppstartOgAvslutning("etterlatte-vedtaksvurdering")
    }

    private val env = System.getenv()
    private val config: Config = ConfigFactory.load()
    private val properties: ApplicationProperties = ApplicationProperties.fromEnv(env)
    private val dataSource = DataSourceBuilder.createDataSource(
        jdbcUrl = properties.jdbcUrl,
        username = properties.dbUsername,
        password = properties.dbPassword
    )

    private val behandlingKlient = BehandlingKlientImpl(config, httpClient())
    private val vedtaksvurderingService = VedtaksvurderingService(
        repository = VedtaksvurderingRepository.using(dataSource),
        beregningKlient = BeregningKlientImpl(config, httpClient()),
        vilkaarsvurderingKlient = VilkaarsvurderingKlientImpl(config, httpClient()),
        behandlingKlient = behandlingKlient,
        sendToRapid = ::publiser
    )

    private val rapidsConnection =
        RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(getRapidEnv()))
            .withKtorModule {
                restModule(sikkerLogg, config = HoconApplicationConfig(config)) {
                    vedtaksvurderingRoute(vedtaksvurderingService, behandlingKlient)
                    automatiskBehandlingRoutes(vedtaksvurderingService, behandlingKlient)
                }
            }
            .build()
            .apply {
                register(object : RapidsConnection.StatusListener {
                    override fun onStartup(rapidsConnection: RapidsConnection) {
                        dataSource.migrate()
                    }
                })
            }

    fun start() = setReady().also { rapidsConnection.start() }

    private fun publiser(melding: String, key: UUID) {
        rapidsConnection.publish(message = melding, key = key.toString())
    }
}