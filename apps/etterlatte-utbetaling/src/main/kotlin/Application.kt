package no.nav.etterlatte

import no.nav.etterlatte.utbetaling.config.ApplicationContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val sikkerLogg: Logger = LoggerFactory.getLogger("sikkerLogg")

fun main() {
    ApplicationContext().also {
        jobs(it)
        rapidApplication(it).start()
        sikkerLogg.info("Utbetaling logger på sikkerlogg")
    }
}

fun jobs(applicationContext: ApplicationContext) {
    applicationContext.grensesnittavstemmingJob.schedule()
    applicationContext.konsistensavstemmingJob.schedule()
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