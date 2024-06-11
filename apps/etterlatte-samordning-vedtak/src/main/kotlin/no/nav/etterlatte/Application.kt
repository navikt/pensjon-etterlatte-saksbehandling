package no.nav.etterlatte

import io.ktor.server.application.install
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.common.logging.sikkerLoggOppstartOgAvslutning
import no.nav.etterlatte.libs.ktor.initialisering.initEmbeddedServer
import no.nav.etterlatte.libs.ktor.setReady
import no.nav.etterlatte.samordning.ApplicationContext
import no.nav.etterlatte.samordning.vedtak.barnepensjonVedtakRoute
import no.nav.etterlatte.samordning.vedtak.samordningVedtakRoute
import no.nav.etterlatte.samordning.vedtak.serverRequestLoggerPlugin
import no.nav.etterlatte.samordning.vedtak.userIdMdcPlugin

fun main() {
    Server(ApplicationContext(Miljoevariabler(System.getenv()))).run()
}

class Server(
    applicationContext: ApplicationContext,
) {
    init {
        sikkerLoggOppstartOgAvslutning("etterlatte-samordning-vedtak")
    }

    private val engine =
        initEmbeddedServer(
            httpPort = applicationContext.httpPort,
            applicationConfig = applicationContext.config,
        ) {
            samordningVedtakRoute(
                samordningVedtakService = applicationContext.samordningVedtakService,
                config = applicationContext.config,
            )

            barnepensjonVedtakRoute(
                samordningVedtakService = applicationContext.samordningVedtakService,
                config = applicationContext.config,
            )

            install(userIdMdcPlugin)
            install(serverRequestLoggerPlugin)
        }

    fun run() = setReady().also { engine.start(true) }
}
