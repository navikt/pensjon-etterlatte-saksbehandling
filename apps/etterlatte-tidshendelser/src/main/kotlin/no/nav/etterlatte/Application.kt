package no.nav.etterlatte

import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.rapidsandrivers.getRapidEnv
import no.nav.etterlatte.tidshendelser.AppContext
import no.nav.etterlatte.tidshendelser.HendelseRiver
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import java.util.Timer

fun main() {
    val rapidEnv = getRapidEnv()
    val miljoevariabler = Miljoevariabler(rapidEnv)

    RapidApplication.create(rapidEnv).also { rapidsConnection ->
        val appContext = AppContext(miljoevariabler) { key, message -> rapidsConnection.publish(key.toString(), message) }

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
    }.start()
}
