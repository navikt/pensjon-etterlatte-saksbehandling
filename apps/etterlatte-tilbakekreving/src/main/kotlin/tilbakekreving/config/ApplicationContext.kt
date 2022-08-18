package no.nav.etterlatte.tilbakekreving.config

import com.typesafe.config.ConfigFactory
import io.ktor.server.auth.AuthenticationConfig
import io.ktor.server.config.HoconApplicationConfig
import no.nav.etterlatte.tilbakekreving.TilbakekrevingDao
import no.nav.etterlatte.tilbakekreving.TilbakekrevingService
import no.nav.etterlatte.tilbakekreving.kravgrunnlag.KravgrunnlagConsumer
import no.nav.etterlatte.tilbakekreving.kravgrunnlag.KravgrunnlagMapper
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.security.token.support.v2.tokenValidationSupport
import java.time.Clock

class ApplicationContext(
    val properties: ApplicationProperties = ApplicationProperties.fromEnv(System.getenv())
) {

    var clock = Clock.systemUTC()

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

    var tokenValidering: AuthenticationConfig.() -> Unit =
        { tokenValidationSupport(config = HoconApplicationConfig(ConfigFactory.load())) }

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