package no.nav.etterlatte

import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.rapidsandrivers.configFromEnvironment
import no.nav.etterlatte.tidshendelser.AppContext
import no.nav.etterlatte.tidshendelser.HendelseRiver
import no.nav.helse.rapids_rivers.RapidsConnection
import rapidsandrivers.initRogR
import java.util.Timer

fun main() =
    initRogR("tidshendelser", configFromEnvironment = { configFromEnvironment(it) }) { rapidsConnection, miljoevariabler ->
        val appContext =
            AppContext(miljoevariabler) { key, message -> rapidsConnection.publish(key.toString(), message) }

        HendelseRiver(rapidsConnection, appContext.hendelseDao)

        rapidsConnection.apply {
            val timers = mutableListOf<Timer>()

            register(
                object : RapidsConnection.StatusListener {
                    override fun onStartup(rapidsConnection: RapidsConnection) {
                        appContext.dataSource.migrate()
                        timers.add(appContext.jobbPollerTask.schedule())
                        timers.add(appContext.hendelsePollerTask.schedule())
                    }

                    override fun onShutdownSignal(rapidsConnection: RapidsConnection) {
                        timers.forEach { it.cancel() }
                    }
                },
            )
        }
    }
