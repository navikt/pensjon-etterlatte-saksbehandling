package no.nav.etterlatte.tilbakekreving.config

import no.nav.etterlatte.tilbakekreving.kravgrunnlag.KravgrunnlagConsumer
import no.nav.etterlatte.tilbakekreving.kravgrunnlag.TilbakekrevingDao
import no.nav.etterlatte.tilbakekreving.kravgrunnlag.TilbakekrevingService
import no.nav.etterlatte.tilbakekreving.kravgrunnlag.KravgrunnlagMapper
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import java.time.Clock

class ApplicationContext(
    val properties: ApplicationProperties = ApplicationProperties.fromEnv(System.getenv()),
    val rapidsConnection: RapidsConnection = RapidApplication.create(System.getenv().withConsumerGroupId())
) {

    var clock = Clock.systemUTC()

    var dataSourceBuilder = DataSourceBuilder(
        jdbcUrl = jdbcUrl(
            host = properties.dbHost,
            port = properties.dbPort,
            databaseName = properties.dbName
        ),
        username = properties.dbUsername,
        password = properties.dbPassword,
    )

    var dataSource = dataSourceBuilder.dataSource()

    var jmsConnectionFactory = JmsConnectionFactory(
        hostname = properties.mqHost,
        port = properties.mqPort,
        queueManager = properties.mqQueueManager,
        channel = properties.mqChannel,
        username = properties.serviceUserUsername,
        password = properties.serviceUserPassword
    )

    var tilbakekrevingDao = TilbakekrevingDao(dataSource)

    var kravgrunnlagMapper = KravgrunnlagMapper()

    var tilbakekrevingService = TilbakekrevingService(
        tilbakekrevingDao = tilbakekrevingDao,
        clock = clock,
        kravgrunnlagMapper = kravgrunnlagMapper
    )

    val kravgrunnlagConsumer: KravgrunnlagConsumer by lazy {
        KravgrunnlagConsumer(
            tilbakekrevingService = tilbakekrevingService,
            jmsConnectionFactory = jmsConnectionFactory,
            queue = properties.mqKravgrunnlagQueue
        )
    }
}

private fun jdbcUrl(host: String, port: Int, databaseName: String) =
    "jdbc:postgresql://${host}:$port/$databaseName"

private fun Map<String, String>.withConsumerGroupId() =
    this.toMutableMap().apply {
        put("KAFKA_CONSUMER_GROUP_ID", get("NAIS_APP_NAME")!!.replace("-", ""))
    }
