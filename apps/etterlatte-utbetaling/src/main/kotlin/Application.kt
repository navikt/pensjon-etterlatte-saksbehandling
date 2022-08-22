package no.nav.etterlatte

import no.nav.etterlatte.utbetaling.config.ApplicationContext
import no.nav.helse.rapids_rivers.RapidsConnection

fun main() {
    ApplicationContext().also {
        jobs(it)
        rapidApplication(it).start()
    }
}

fun jobs(applicationContext: ApplicationContext) {
    applicationContext.grensesnittavstemmingJob.schedule()
}

fun rapidApplication(applicationContext: ApplicationContext): RapidsConnection =
    applicationContext.rapidsConnection
        .apply {
            applicationContext.vedtakMottaker
            applicationContext.kvitteringMottaker
            applicationContext.oppgavetrigger

            register(object : RapidsConnection.StatusListener {
                override fun onStartup(rapidsConnection: RapidsConnection) {
                    applicationContext.dataSourceBuilder.migrate()
                }

                override fun onShutdown(rapidsConnection: RapidsConnection) {
                    applicationContext.jmsConnectionFactory.stop()
                }
            })
        }