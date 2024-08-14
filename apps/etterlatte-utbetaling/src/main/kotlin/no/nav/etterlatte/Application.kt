package no.nav.etterlatte

import io.ktor.server.config.HoconApplicationConfig
import no.nav.etterlatte.libs.common.TimerJob
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.etterlatte.libs.ktor.setReady
import no.nav.etterlatte.rapidsandrivers.configFromEnvironment
import no.nav.etterlatte.utbetaling.common.OppgavetriggerRiver
import no.nav.etterlatte.utbetaling.config.ApplicationContext
import no.nav.etterlatte.utbetaling.iverksetting.KvitteringMottaker
import no.nav.etterlatte.utbetaling.iverksetting.VedtakMottakRiver
import no.nav.etterlatte.utbetaling.utbetalingRoutes
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.Logger
import rapidsandrivers.initRogR

val sikkerLogg: Logger = sikkerlogger()

fun main() {
    ApplicationContext().also {
        initRogR(
            applikasjonsnavn = "utbetaling",
            restModule = {
                restModule(sikkerLogg, config = HoconApplicationConfig(it.config)) {
                    utbetalingRoutes(it.simuleringOsService, it.behandlingKlient)
                }
            },
            configFromEnvironment = { configFromEnvironment(it) },
            setReady = { setReady() },
        ) { rc, _ -> rc.settOppRiversOgListener(it) }
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
    return jobs
}

internal fun RapidsConnection.settOppRiversOgListener(applicationContext: ApplicationContext) {
    VedtakMottakRiver(
        rapidsConnection = this,
        utbetalingService = applicationContext.utbetalingService,
    )
    KvitteringMottaker(
        rapidsConnection = this,
        utbetalingService = applicationContext.utbetalingService,
        jmsConnectionFactory = applicationContext.jmsConnectionFactory,
        queue = applicationContext.properties.mqKvitteringQueue,
    )
    OppgavetriggerRiver(
        rapidsConnection = this,
        utbetalingService = applicationContext.utbetalingService,
        grensesnittsavstemmingService = applicationContext.grensesnittsavstemmingService,
    )

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
