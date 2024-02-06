package no.nav.etterlatte

import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.tidshendelser.AppContext
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import rapidsandrivers.getRapidEnv
import java.util.Timer

fun main() {
    val rapidEnv = getRapidEnv()
    val miljoevariabler = Miljoevariabler(rapidEnv)

    RapidApplication.create(rapidEnv).also { rapidsConnection ->
        val appContext = AppContext(miljoevariabler)

        rapidsConnection.apply {
            val timers = mutableListOf<Timer>()

            register(
                object : RapidsConnection.StatusListener {
                    override fun onStartup(rapidsConnection: RapidsConnection) {
                        appContext.dataSource.migrate()
                        timers.add(appContext.jobbPoller.start())
                        timers.add(appContext.hendelsePoller.start())
                    }

                    override fun onShutdownSignal(rapidsConnection: RapidsConnection) {
                        timers.forEach { it.cancel() }
                    }
                },
            )
        }
    }.start()
}
