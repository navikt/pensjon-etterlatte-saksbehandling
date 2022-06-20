package no.nav.etterlatte

import no.nav.etterlatte.tilbakekreving.config.ApplicationContext
import no.nav.helse.rapids_rivers.RapidsConnection

fun main() {
    ApplicationContext().also {
        rapidApplication(it).start()
    }
}

fun rapidApplication(applicationContext: ApplicationContext): RapidsConnection =
    applicationContext.rapidsConnection
        .apply {
            applicationContext.tilbakekrevingConsumer(
                tilbakekrevingService = applicationContext.tilbakekrevingService,
                jmsConnectionFactory = applicationContext.jmsConnectionFactory
            ).start()

            register(object : RapidsConnection.StatusListener {
                override fun onStartup(rapidsConnection: RapidsConnection) {
                    applicationContext.dataSourceBuilder.migrate()
                }

                override fun onShutdown(rapidsConnection: RapidsConnection) {
                    applicationContext.jmsConnectionFactory.stop()
                }
            })
        }