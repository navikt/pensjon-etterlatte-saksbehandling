package no.nav.etterlatte.tilbakekreving.config

import no.nav.etterlatte.tilbakekreving.TilbakekrevingConsumer
import no.nav.etterlatte.tilbakekreving.TilbakekrevingDao
import no.nav.etterlatte.tilbakekreving.TilbakekrevingService
import java.time.Clock

class ApplicationContext(
    properties: ApplicationProperties = ApplicationProperties.fromEnv(System.getenv()),
) {

    val clock = Clock.systemUTC()

    val dataSourceBuilder = DataSourceBuilder(
        jdbcUrl = jdbcUrl(
            host = properties.dbHost,
            port = properties.dbPort,
            databaseName = properties.dbName
        ),
        username = properties.dbUsername,
        password = properties.dbPassword,
    )

    val dataSource = dataSourceBuilder.dataSource()

    val jmsConnectionFactory = JmsConnectionFactory(
        hostname = properties.mqHost,
        port = properties.mqPort,
        queueManager = properties.mqQueueManager,
        channel = properties.mqChannel,
        username = properties.serviceUserUsername,
        password = properties.serviceUserPassword
    )

    val tilbakekrevingDao = TilbakekrevingDao(dataSource)

    val tilbakekrevingService = TilbakekrevingService(
        tilbakekrevingDao = tilbakekrevingDao,
        clock = clock
    )

    val tilbakekrevingConsumer by lazy {
        TilbakekrevingConsumer(
            tilbakekrevingService = tilbakekrevingService,
            jmsConnectionFactory = jmsConnectionFactory,
            queue = properties.mqKravgrunnlagQueue
        )
    }

    private fun jdbcUrl(host: String, port: Int, databaseName: String) =
        "jdbc:postgresql://${host}:$port/$databaseName"
}