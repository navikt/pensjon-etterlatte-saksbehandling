package rapidsandrivers

import com.github.navikt.tbd_libs.kafka.ConsumerProducerFactory
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.ktor.server.application.Application
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.common.logging.sikkerLoggOppstart
import no.nav.etterlatte.rapidsandrivers.configFromEnvironment
import no.nav.etterlatte.rapidsandrivers.getRapidEnv
import no.nav.helse.rapids_rivers.RapidApplication

fun newInitRogR(
    applikasjonsnavn: String,
    restModule: (Application.() -> Unit)? = null,
    settOppRivers: (RapidsConnection, Miljoevariabler) -> Unit,
) {
    sikkerLoggOppstart("etterlatte-$applikasjonsnavn")

    val rapidEnv = getRapidEnv()
    val config = configFromEnvironment(rapidEnv)

    RapidApplication
        .create(
            env = rapidEnv.props,
            builder = {
                if (restModule != null) {
                    withKtorModule(restModule)
                }
                withIsAliveEndpoint("/health/isalive")
                withIsReadyEndpoint("/health/isready")
            },
            consumerProducerFactory = ConsumerProducerFactory(config),
        ).apply { settOppRivers(this, rapidEnv) }
        .start()
//
//    var builder =
//        RapidApplication.Builder(
//            RapidApplication.RapidApplicationConfig.fromEnv(
//                rapidEnv.props,
//                configFromEnvironment(rapidEnv),
//            ),
//        )
//    if (restModule != null) {
//        builder = builder.withKtorModule(restModule)
//    }
}
