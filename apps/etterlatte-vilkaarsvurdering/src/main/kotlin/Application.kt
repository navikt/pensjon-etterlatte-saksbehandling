package no.nav.etterlatte

import no.nav.etterlatte.libs.common.logging.sikkerLoggOppstartOgAvslutning
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.ktor.initialisering.initEmbeddedServer
import no.nav.etterlatte.libs.ktor.initialisering.run
import no.nav.etterlatte.vilkaarsvurdering.aldersovergang
import no.nav.etterlatte.vilkaarsvurdering.config.ApplicationContext
import no.nav.etterlatte.vilkaarsvurdering.vilkaarsvurdering

fun main() {
    ApplicationContext().let { Server(it).run() }
}

class Server(
    private val context: ApplicationContext,
) {
    init {
        sikkerLoggOppstartOgAvslutning("etterlatte-vilkaarsvurdering")
    }

    private val engine =
        with(context) {
            initEmbeddedServer(
                httpPort = properties.httpPort,
                applicationConfig = context.config,
            ) {
                vilkaarsvurdering(vilkaarsvurderingService)
                aldersovergang(aldersovergangService)
            }
        }

    fun run() =
        with(context) {
            dataSource.migrate()
            engine.run()
        }
}
