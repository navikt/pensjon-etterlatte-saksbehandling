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
    kafkaConsumerGroupName: String? = null,
    restModule: (Application.() -> Unit)? = null,
    configFromEnvironment: (Miljoevariabler) -> Config = { AivenConfig.default },
    settOppRivers: (RapidsConnection, rapidEnv: Miljoevariabler) -> Unit,
) {
    sikkerLoggOppstartOgAvslutning("etterlatte-$applikasjonsnavn")

    val rapidEnv =
        if (kafkaConsumerGroupName != null) {
            getRapidEnv(kafkaConsumerGroupName)
        } else {
            getRapidEnv()
        }

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
            .withIsAliveEndpoint("/health/isalive")
            .withIsReadyEndpoint("/health/isready")
            .build()
            .also { rapidsConnection -> settOppRivers(rapidsConnection, rapidEnv) }
    connection.start()
}
