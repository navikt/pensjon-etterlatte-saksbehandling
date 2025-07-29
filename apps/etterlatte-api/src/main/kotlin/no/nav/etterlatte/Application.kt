package no.nav.etterlatte

import io.ktor.server.plugins.swagger.swaggerUI
import no.nav.etterlatte.behandling.sak.behandlingSakRoutes
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.common.appName
import no.nav.etterlatte.libs.common.logging.sikkerLoggOppstart
import no.nav.etterlatte.libs.ktor.initialisering.initEmbeddedServer
import no.nav.etterlatte.libs.ktor.initialisering.runEngine
import no.nav.etterlatte.oppgave.oppgaveRoute
import no.nav.etterlatte.samordning.serverRequestLoggerPlugin
import no.nav.etterlatte.samordning.userIdMdcPlugin
import no.nav.etterlatte.samordning.vedtak.barnepensjonVedtakRoute
import no.nav.etterlatte.samordning.vedtak.samordningVedtakRoute
import no.nav.etterlatte.vedtak.vedtakRoute

fun main() {
    Server(ApplicationContext(Miljoevariabler.systemEnv())).runServer()
}

class Server(
    applicationContext: ApplicationContext,
) {
    init {
        sikkerLoggOppstart("etterlatte-api")
    }

    private val engine =
        initEmbeddedServer(
            httpPort = applicationContext.httpPort,
            applicationConfig = applicationContext.config,
            routes = {
                swaggerUI(path = "api/v1/vedtak/swagger", swaggerFile = "vedtakSwaggerV1.yaml")
            },
        ) {
            samordningVedtakRoute(
                samordningVedtakService = applicationContext.samordningVedtakService,
                config = applicationContext.config,
                appname = appName()!!,
            )

            barnepensjonVedtakRoute(
                samordningVedtakService = applicationContext.samordningVedtakService,
                config = applicationContext.config,
            )

            behandlingSakRoutes(
                behandlingService = applicationContext.behandlingService,
                config = applicationContext.config,
            )

            vedtakRoute(vedtakService = applicationContext.vedtakService)

            oppgaveRoute(applicationContext.oppgaveService)

            install(userIdMdcPlugin)
            install(serverRequestLoggerPlugin)
        }

    fun runServer() = engine.runEngine()
}
