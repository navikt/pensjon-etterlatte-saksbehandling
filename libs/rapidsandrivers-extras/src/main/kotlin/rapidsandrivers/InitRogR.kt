package rapidsandrivers

import io.ktor.server.application.Application
import no.nav.etterlatte.libs.common.logging.sikkerLoggOppstartOgAvslutning
import no.nav.etterlatte.rapidsandrivers.getRapidEnv
import no.nav.helse.rapids_rivers.AivenConfig
import no.nav.helse.rapids_rivers.Config
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection

fun initRogR(
    applikasjonsnavn: String,
    restModule: (Application.() -> Unit)? = null,
    configFromEnvironment: ((Map<String, String>) -> Config) = { AivenConfig.default },
    settOppRivers: (RapidsConnection, rapidEnv: Map<String, String>) -> Unit,
) {
    sikkerLoggOppstartOgAvslutning("etterlatte-$applikasjonsnavn")
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

    val connection =
        builder
            .build()
            .also { rapidsConnection -> settOppRivers(rapidsConnection, rapidEnv) }
    connection.start()
}
