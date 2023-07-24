package no.nav.etterlatte

import no.nav.etterlatte.jobs.addShutdownHook
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.utbetaling.config.ApplicationContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

val sikkerLogg: Logger = LoggerFactory.getLogger("sikkerLogg")

fun main() {
    ApplicationContext().also {
        jobs(it)
        rapidApplication(it).start()
        sikkerLogg.info("Utbetaling logger på sikkerlogg")
    }
}

fun jobs(applicationContext: ApplicationContext) {
    val jobs = mutableSetOf<Timer>()
    with(applicationContext.grensesnittavstemmingJob) {
        if (applicationContext.properties.grensesnittavstemmingEnabled) schedule().also { jobs.add(it) }
    }
    with(applicationContext.grensesnittavstemmingJobOMS) {
        if (applicationContext.properties.grensesnittavstemmingOMSEnabled) schedule().also { jobs.add(it) }
    }
    with(applicationContext.konsistensavstemmingJob) {
        if (applicationContext.properties.konsistensavstemmingEnabled) schedule().also { jobs.add(it) }
    }
    with(applicationContext.konsistensavstemmingJobOMS) {
        if (applicationContext.properties.konsistensavstemmingOMSEnabled) schedule().also { jobs.add(it) }
    }
    addShutdownHook(jobs)
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