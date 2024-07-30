package rapidsandrivers

import io.ktor.server.application.Application
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.common.logging.sikkerLoggOppstartOgAvslutning
import no.nav.etterlatte.rapidsandrivers.getRapidEnv
import no.nav.helse.rapids_rivers.AivenConfig
import no.nav.helse.rapids_rivers.Config
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection

fun initRogR(
    applikasjonsnavn: String,
    restModule: (Application.() -> Unit)? = null,
    configFromEnvironment: ((Miljoevariabler) -> Config) = { AivenConfig.default },
    setReady: () -> Unit = {},
    settOppRivers: (RapidsConnection, rapidEnv: Miljoevariabler) -> Unit,
) {
    sikkerLoggOppstartOgAvslutning("etterlatte-$applikasjonsnavn")
    val rapidEnv = getRapidEnv()

    var builder =
        RapidApplication.Builder(
            RapidApplication.RapidApplicationConfig.fromEnv(
                rapidEnv.props,
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
    setReady()
    connection.start()
}
