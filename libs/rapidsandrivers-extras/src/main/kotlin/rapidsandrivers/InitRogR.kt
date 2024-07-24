package rapidsandrivers

import io.ktor.server.application.Application
import no.nav.etterlatte.rapidsandrivers.getRapidEnv
import no.nav.helse.rapids_rivers.AivenConfig
import no.nav.helse.rapids_rivers.Config
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection

fun initRogR(
    restModule: (Application.() -> Unit)? = null,
    configFromEnvironment: ((Map<String, String>) -> Config) = { AivenConfig.default },
    settOppRivers: (RapidsConnection, rapidEnv: Map<String, String>) -> Unit,
) {
    val rapidEnv = getRapidEnv()

    var builder =
        RapidApplication.Builder(
            RapidApplication.RapidApplicationConfig.fromEnv(
                rapidEnv,
                configFromEnvironment(rapidEnv),
            ),
        )
    if (restModule != null) {
        builder = builder.withKtorModule(restModule)
    }

    builder
        .build()
        .also { rapidsConnection -> settOppRivers(rapidsConnection, rapidEnv) }
        .start()
}
