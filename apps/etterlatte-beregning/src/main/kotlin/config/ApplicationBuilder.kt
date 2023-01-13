package no.nav.etterlatte.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import no.nav.etterlatte.BeregningRepositoryImpl
import no.nav.etterlatte.beregning
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.ktor.httpClient
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.etterlatte.model.BeregningService
import no.nav.etterlatte.model.VilkaarsvurderingKlientImpl
import no.nav.etterlatte.model.behandling.BehandlingKlientImpl
import no.nav.etterlatte.model.grunnlag.GrunnlagKlientImpl
import no.nav.helse.rapids_rivers.RapidApplication

class ApplicationBuilder {
    private val env = System.getenv()
    private val properties: ApplicationProperties = ApplicationProperties.fromEnv(env)
    private val dataSource = DataSourceBuilder.createDataSource(env).also { it.migrate() }
    private val config: Config = ConfigFactory.load()

    private val beregningRepository = BeregningRepositoryImpl(dataSource)
    private val vilkaarsvurderingKlientImpl = VilkaarsvurderingKlientImpl(config, httpClient())
    private val grunnlagKlientImpl = GrunnlagKlientImpl(config, httpClient())
    private val behandlingKlient = BehandlingKlientImpl(config, httpClient())
    private val beregningService = BeregningService(
        beregningRepository,
        vilkaarsvurderingKlientImpl,
        grunnlagKlientImpl,
        behandlingKlient
    )
    private val rapidsConnection =
        RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(env.withConsumerGroupId()))
            .withKtorModule {
                restModule {
                    beregning(beregningService)
                }
            }
            .build()

    fun start() = rapidsConnection.start()
}

fun Map<String, String>.withConsumerGroupId() =
    this.toMutableMap().apply {
        put("KAFKA_CONSUMER_GROUP_ID", get("NAIS_APP_NAME")!!.replace("-", ""))
    }