package no.nav.etterlatte.tilbakekreving.config

import com.fasterxml.jackson.databind.SerializationFeature
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials
import no.nav.etterlatte.mq.JmsConnectionFactory
import no.nav.etterlatte.tilbakekreving.TilbakekrevingHendelseRepository
import no.nav.etterlatte.tilbakekreving.TilbakekrevingService
import no.nav.etterlatte.tilbakekreving.klienter.BehandlingKlient
import no.nav.etterlatte.tilbakekreving.klienter.TilbakekrevingskomponentenKlient
import no.nav.etterlatte.tilbakekreving.kravgrunnlag.KravgrunnlagConsumer
import no.nav.etterlatte.tilbakekreving.kravgrunnlag.KravgrunnlagService

class ApplicationContext(
    val properties: ApplicationProperties = ApplicationProperties.fromEnv(System.getenv()),
) {
    val dataSource =
        DataSourceBuilder.createDataSource(
            jdbcUrl = properties.jdbcUrl,
            username = properties.dbUsername,
            password = properties.dbPassword,
        )

    var jmsConnectionFactory =
        JmsConnectionFactory(
            hostname = properties.mqHost,
            port = properties.mqPort,
            queueManager = properties.mqQueueManager,
            channel = properties.mqChannel,
            username = properties.serviceUserUsername,
            password = properties.serviceUserPassword,
        )

    val behandlingKlient =
        BehandlingKlient(
            url = properties.behandlingUrl,
            httpClient =
                httpClientClientCredentials(
                    azureAppClientId = properties.azureAppClientId,
                    azureAppJwk = properties.azureAppJwk,
                    azureAppWellKnownUrl = properties.azureAppWellKnownUrl,
                    azureAppScope = properties.behandlingScope,
                    ekstraJacksoninnstillinger = { it.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) },
                ),
        )

    val tilbakekrevingHendelseRepository = TilbakekrevingHendelseRepository(dataSource)

    val tilbakekrevingKlient =
        TilbakekrevingskomponentenKlient(
            url = properties.proxyUrl,
            httpClient =
                httpClientClientCredentials(
                    azureAppClientId = properties.azureAppClientId,
                    azureAppJwk = properties.azureAppJwk,
                    azureAppWellKnownUrl = properties.azureAppWellKnownUrl,
                    azureAppScope = properties.proxyScope,
                ),
            hendelseRepository = tilbakekrevingHendelseRepository,
        )

    val tilbakekrevingService = TilbakekrevingService(tilbakekrevingKlient)

    val kravgrunnlagService = KravgrunnlagService(behandlingKlient)

    val kravgrunnlagConsumer =
        KravgrunnlagConsumer(
            connectionFactory = jmsConnectionFactory,
            queue = properties.mqKravgrunnlagQueue,
            kravgrunnlagService = kravgrunnlagService,
            hendelseRepository = tilbakekrevingHendelseRepository,
        )
}
