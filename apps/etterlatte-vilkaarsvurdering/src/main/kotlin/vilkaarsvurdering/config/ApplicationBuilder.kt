package no.nav.etterlatte.vilkaarsvurdering.config

import no.nav.etterlatte.restModule
import no.nav.etterlatte.vilkaarsvurdering.GrunnlagEndretRiver
import no.nav.etterlatte.vilkaarsvurdering.VilkaarsvurderingRepositoryInMemory
import no.nav.etterlatte.vilkaarsvurdering.VilkaarsvurderingService
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection

class ApplicationBuilder {
    private val vilkaarsvurderingRepository = VilkaarsvurderingRepositoryInMemory()
    val vilkaarsvurderingService = VilkaarsvurderingService(vilkaarsvurderingRepository, ::publiser)

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
    private fun publiser(melding: String) {
        rapidsConnection.publish(message = melding)
    }

    /* val properties: ApplicationProperties = ApplicationProperties.fromEnv(System.getenv())
var dataSourceBuilder = DataSourceBuilder(
    jdbcUrl = jdbcUrl(
        host = properties.dbHost,
        port = properties.dbPort,
        databaseName = properties.dbName
    ),
    username = properties.dbUsername,
    password = properties.dbPassword
)

var dataSource = dataSourceBuilder.dataSource()

private fun jdbcUrl(host: String, port: Int, databaseName: String) =
"jdbc:postgresql://$host:$port/$databaseName"
*/
}

fun Map<String, String>.withConsumerGroupId() =
    this.toMutableMap().apply {
        put("KAFKA_CONSUMER_GROUP_ID", get("NAIS_APP_NAME")!!.replace("-", ""))
    }