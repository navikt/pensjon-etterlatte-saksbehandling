package no.nav.etterlatte

import no.nav.etterlatte.libs.common.logging.sikkerLoggOppstart
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.ktor.initialisering.initEmbeddedServer
import no.nav.etterlatte.libs.ktor.initialisering.runEngine
import no.nav.etterlatte.trygdetid.avtale.avtale
import no.nav.etterlatte.trygdetid.config.ApplicationContext
import no.nav.etterlatte.trygdetid.trygdetid

fun main() {
    ApplicationContext().let { Server(it).runServer() }
}

class Server(
    private val context: ApplicationContext,
) {
    init {
        sikkerLoggOppstart("etterlatte-trygdetid")
    }

    private val engine =
        with(context) {
            initEmbeddedServer(
                httpPort = properties.httpPort,
                applicationConfig = context.config,
                cronJobs =
                    listOf(
                        context.sjekkAvvikJobb,
                    ),
            ) {
                trygdetid(trygdetidService, behandlingKlient, featureToggleService)
                avtale(avtaleService, behandlingKlient)
            }
        }

    fun runServer() =
        with(context) {
            dataSource.migrate()
            engine.runEngine()
        }
}
