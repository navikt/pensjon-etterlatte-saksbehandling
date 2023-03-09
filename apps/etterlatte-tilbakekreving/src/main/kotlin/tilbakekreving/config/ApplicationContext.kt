package no.nav.etterlatte.tilbakekreving.config

import no.nav.etterlatte.libs.common.tidspunkt.utcKlokke
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.tilbakekreving.TilbakekrevingDao
import no.nav.etterlatte.tilbakekreving.TilbakekrevingService
import no.nav.etterlatte.tilbakekreving.kravgrunnlag.KravgrunnlagConsumer
import no.nav.etterlatte.tilbakekreving.kravgrunnlag.KravgrunnlagMapper
import no.nav.helse.rapids_rivers.RapidsConnection

class ApplicationContext(
    val properties: ApplicationProperties = ApplicationProperties.fromEnv(System.getenv())
) {

    var clock = utcKlokke()

    var dataSource = DataSourceBuilder.createDataSource(
        jdbcUrl = jdbcUrl(
            host = properties.dbHost,
            port = properties.dbPort,
            databaseName = properties.dbName
        ),
        username = properties.dbUsername,
        password = properties.dbPassword
    )

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

    fun kravgrunnlagConsumer(rapidsConnection: RapidsConnection) =
        KravgrunnlagConsumer(
            rapidsConnection = rapidsConnection,
            tilbakekrevingService = tilbakekrevingService,
            jmsConnectionFactory = jmsConnectionFactory,
            queue = properties.mqKravgrunnlagQueue
        )
}

private fun jdbcUrl(host: String, port: Int, databaseName: String) =
    "jdbc:postgresql://$host:$port/$databaseName"