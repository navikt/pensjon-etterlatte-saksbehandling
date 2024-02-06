package no.nav.etterlatte

import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.tidshendelser.AppContext
import no.nav.etterlatte.rapidsandrivers.getRapidEnv
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection

fun main() {
    val rapidEnv = getRapidEnv()
    val miljoevariabler = Miljoevariabler(rapidEnv)

    RapidApplication.create(rapidEnv).also { rapidsConnection ->
        val appContext = AppContext(miljoevariabler)

        rapidsConnection.apply {
            register(
                object : RapidsConnection.StatusListener {
                    override fun onStartup(rapidsConnection: RapidsConnection) {
                        appContext.dataSource.migrate()
                        appContext.jobbPoller.start()
                    }
                },
            )
        }
    }.start()
}
