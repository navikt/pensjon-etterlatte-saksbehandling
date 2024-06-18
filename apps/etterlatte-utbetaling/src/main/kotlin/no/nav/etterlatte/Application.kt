package no.nav.etterlatte

import no.nav.etterlatte.libs.common.TimerJob
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.utbetaling.config.ApplicationContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.Logger

val sikkerLogg: Logger = sikkerlogger()

fun main() {
    ApplicationContext().also {
        rapidApplication(it).start()
        sikkerLogg.info("Utbetaling logger på sikkerlogg")
    }
}

fun jobs(applicationContext: ApplicationContext): MutableSet<TimerJob> {
    val jobs = mutableSetOf<TimerJob>()
    if (applicationContext.properties.grensesnittavstemmingEnabled) {
        jobs.add(applicationContext.grensesnittavstemmingJob)
    }
    if (applicationContext.properties.grensesnittavstemmingOMSEnabled) {
        jobs.add(applicationContext.grensesnittavstemmingJobOMS)
    }
    if (applicationContext.properties.konsistensavstemmingEnabled) {
        jobs.add(applicationContext.konsistensavstemmingJob)
    }
    if (applicationContext.properties.konsistensavstemmingOMSEnabled) {
        jobs.add(applicationContext.konsistensavstemmingJobOMS)
    }
    jobs.add(applicationContext.verifiserUtbetalingOgVedtakJob)
    return jobs
}

fun rapidApplication(applicationContext: ApplicationContext): RapidsConnection =
    applicationContext.rapidsConnection
        .apply {
            applicationContext.vedtakMottakRiver
            applicationContext.kvitteringMottaker
            applicationContext.oppgavetriggerRiver

            register(
                object : RapidsConnection.StatusListener {
                    override fun onStartup(rapidsConnection: RapidsConnection) {
                        applicationContext.dataSource.migrate().also {
                            val cronjobs = jobs(applicationContext)
                            val timerJobs = cronjobs.map { job -> job.schedule() }
                            addShutdownHook(timerJobs)
                        }
                    }

                    override fun onShutdown(rapidsConnection: RapidsConnection) {
                        applicationContext.jmsConnectionFactory.stop()
                    }
                },
            )
        }
