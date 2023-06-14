package no.nav.etterlatte

import no.nav.etterlatte.libs.database.migrate
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
    with(applicationContext.grensesnittavstemmingJob) {
        if (applicationContext.properties.grensesnittavstemmingEnabled) schedule()
    }
    with(applicationContext.grensesnittavstemmingJobOMS) {
        if (applicationContext.properties.grensesnittavstemmingOMSEnabled) schedule()
    }
    with(applicationContext.konsistensavstemmingJob) {
        if (applicationContext.properties.konsistensavstemmingEnabled) schedule()
    }
    with(applicationContext.konsistensavstemmingJobOMS) {
        if (applicationContext.properties.konsistensavstemmingOMSEnabled) schedule()
    }
}

fun rapidApplication(applicationContext: ApplicationContext): RapidsConnection =
    applicationContext.rapidsConnection
        .apply {
            applicationContext.vedtakMottaker
            applicationContext.kvitteringMottaker
            applicationContext.oppgavetrigger

            register(object : RapidsConnection.StatusListener {
                override fun onStartup(rapidsConnection: RapidsConnection) {
                    applicationContext.dataSource.migrate()
                }

                override fun onShutdown(rapidsConnection: RapidsConnection) {
                    applicationContext.jmsConnectionFactory.stop()
                }
            })
        }