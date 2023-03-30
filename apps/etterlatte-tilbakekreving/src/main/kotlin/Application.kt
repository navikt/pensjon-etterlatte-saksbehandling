package no.nav.etterlatte

import com.typesafe.config.ConfigFactory
import io.ktor.server.config.HoconApplicationConfig
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.etterlatte.tilbakekreving.config.ApplicationContext
import no.nav.etterlatte.tilbakekreving.tilbakekreving
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import rapidsandrivers.getRapidEnv

val sikkerLogg: Logger = LoggerFactory.getLogger("sikkerLogg")

fun main() {
    ApplicationContext().also {
        rapidApplication(it).start()
    }
}

fun rapidApplication(
    applicationContext: ApplicationContext,
    rapidsConnection: RapidsConnection =
        RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(getRapidEnv()))
            .withKtorModule {
                restModule(sikkerLogg, config = HoconApplicationConfig(ConfigFactory.load())) {
                    tilbakekreving(applicationContext.tilbakekrevingService)
                }
            }
            .build()
): RapidsConnection =
    with(applicationContext) {
        rapidsConnection.register(object : RapidsConnection.StatusListener {
            override fun onStartup(rapidsConnection: RapidsConnection) {
                dataSource.migrate()
                // kravgrunnlagConsumer(rapidsConnection).start() TODO trenger å sette opp kø
            }

            override fun onShutdown(rapidsConnection: RapidsConnection) {
                jmsConnectionFactory.stop()
            }
        })
        rapidsConnection
    }