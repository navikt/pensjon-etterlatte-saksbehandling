package no.nav.etterlatte

import no.nav.etterlatte.avkorting.avkorting
import no.nav.etterlatte.beregning.beregning
import no.nav.etterlatte.beregning.grunnlag.beregningsGrunnlag
import no.nav.etterlatte.config.ApplicationContext
import no.nav.etterlatte.grunnbeloep.grunnbeloep
import no.nav.etterlatte.libs.common.logging.sikkerLoggOppstartOgAvslutning
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.ktor.initialisering.initEmbeddedServer
import no.nav.etterlatte.libs.ktor.setReady
import no.nav.etterlatte.sanksjon.sanksjon
import no.nav.etterlatte.ytelseMedGrunnlag.ytelseMedGrunnlag

fun main() {
    ApplicationContext().let { Server(it).run() }
}

class Server(
    private val context: ApplicationContext,
) {
    init {
        sikkerLoggOppstartOgAvslutning("etterlatte-beregning")
    }

    private val engine =
        with(context) {
            initEmbeddedServer(
                httpPort = properties.httpPort,
                applicationConfig = context.config,
            ) {
                beregning(beregningService, behandlingKlient)
                beregningsGrunnlag(beregningsGrunnlagService, behandlingKlient)
                avkorting(avkortingService, behandlingKlient)
                ytelseMedGrunnlag(ytelseMedGrunnlagService, behandlingKlient)
                grunnbeloep(grunnbeloepService)
                sanksjon(sanksjonService, behandlingKlient)
            }
        }

    fun run() =
        with(context) {
            dataSource.migrate()
            setReady()
            engine.start(true)
        }
}
