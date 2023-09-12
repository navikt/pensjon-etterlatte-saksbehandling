package no.nav.etterlatte.tilbakekreving.config

import no.nav.etterlatte.tilbakekreving.klienter.BehandlingKlient
import no.nav.etterlatte.tilbakekreving.kravgrunnlag.KravgrunnlagConsumer
import no.nav.etterlatte.tilbakekreving.kravgrunnlag.KravgrunnlagService

class ApplicationContext(
    properties: ApplicationProperties = ApplicationProperties.fromEnv(System.getenv())
) {
    var jmsConnectionFactory = JmsConnectionFactory(
        hostname = properties.mqHost,
        port = properties.mqPort,
        queueManager = properties.mqQueueManager,
        channel = properties.mqChannel,
        username = properties.serviceUserUsername,
        password = properties.serviceUserPassword
    )

    var kravgrunnlagConsumer =
        KravgrunnlagConsumer(
            connectionFactory = jmsConnectionFactory,
            queue = properties.mqKravgrunnlagQueue,
            kravgrunnlagService = KravgrunnlagService(BehandlingKlient())
        )
}