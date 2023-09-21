package no.nav.etterlatte

import io.ktor.server.cio.CIO
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import no.nav.etterlatte.avkorting.avkorting
import no.nav.etterlatte.beregning.beregning
import no.nav.etterlatte.beregning.grunnlag.beregningsGrunnlag
import no.nav.etterlatte.config.ApplicationContext
import no.nav.etterlatte.libs.common.logging.sikkerLoggOppstartOgAvslutning
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.etterlatte.libs.ktor.setReady
import no.nav.etterlatte.ytelseMedGrunnlag.ytelseMedGrunnlag
import org.slf4j.Logger

val sikkerLogg: Logger = sikkerlogger()

fun main() {
    ApplicationContext().let { Server(it).run() }
}

class Server(private val context: ApplicationContext) {
    init {
        sikkerLoggOppstartOgAvslutning("etterlatte-beregning")
    }

    private val engine =
        with(context) {
            embeddedServer(
                factory = CIO,
                environment =
                    applicationEngineEnvironment {
                        config = HoconApplicationConfig(context.config)
                        module {
                            restModule(sikkerLogg, withMetrics = true) {
                                beregning(beregningService, behandlingKlient)
                                beregningsGrunnlag(beregningsGrunnlagService, behandlingKlient)
                                avkorting(avkortingService, behandlingKlient)
                                ytelseMedGrunnlag(ytelseMedGrunnlagService, behandlingKlient)
                            }
                        }
                        connector { port = properties.httpPort }
                    },
            )
        }

    fun run() =
        with(context) {
            dataSource.migrate()
            setReady()
            engine.start(true)
        }
}
