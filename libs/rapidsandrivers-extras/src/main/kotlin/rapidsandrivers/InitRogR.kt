package rapidsandrivers

import io.ktor.server.application.Application
import no.nav.etterlatte.rapidsandrivers.getRapidEnv
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection

fun initRogR(
    restModule: Application.() -> Unit,
    settOppRivers: (RapidsConnection) -> Unit,
) {
    val rapidEnv = getRapidEnv()

    RapidApplication
        .Builder(RapidApplication.RapidApplicationConfig.fromEnv(rapidEnv))
        .withKtorModule(restModule)
        .build()
        .also { rapidsConnection ->
            settOppRivers(rapidsConnection)
        }.start()
}
