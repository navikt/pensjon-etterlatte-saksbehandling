package no.nav.etterlatte.vilkaarsvurdering.config

import no.nav.etterlatte.restModule
import no.nav.etterlatte.vilkaarsvurdering.GrunnlagEndretRiver
import no.nav.etterlatte.vilkaarsvurdering.VilkaarsvurderingRepositoryImpl
import no.nav.etterlatte.vilkaarsvurdering.VilkaarsvurderingService
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import java.util.*

class ApplicationBuilder {
    private val properties: ApplicationProperties = ApplicationProperties.fromEnv(System.getenv())

    private val dataSourceBuilder = DataSourceBuilder(
        jdbcUrl = properties.jdbcUrl,
        username = properties.dbUsername,
        password = properties.dbPassword
    ).apply { migrate() }

    private val dataSource = dataSourceBuilder.dataSource()
    private val vilkaarsvurderingRepository = VilkaarsvurderingRepositoryImpl(dataSource)
    private val vilkaarsvurderingService = VilkaarsvurderingService(vilkaarsvurderingRepository, ::publiser)

    private val rapidsConnection =
        RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(System.getenv().withConsumerGroupId()))
            .withKtorModule { restModule(vilkaarsvurderingService = vilkaarsvurderingService) }
            .build()
            .apply {
                register(object : RapidsConnection.StatusListener {
                    override fun onStartup(rapidsConnection: RapidsConnection) {}
                    override fun onShutdown(rapidsConnection: RapidsConnection) {}
                })

                GrunnlagEndretRiver(rapidsConnection = this, vilkaarsvurderingService = vilkaarsvurderingService)
            }

    fun start() = rapidsConnection.start()
    private fun publiser(melding: String, key: UUID) {
        rapidsConnection.publish(message = melding, key = key.toString())
    }
}

fun Map<String, String>.withConsumerGroupId() =
    this.toMutableMap().apply {
        put("KAFKA_CONSUMER_GROUP_ID", get("NAIS_APP_NAME")!!.replace("-", ""))
    }