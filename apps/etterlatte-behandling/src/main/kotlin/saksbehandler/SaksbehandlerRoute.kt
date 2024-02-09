package no.nav.etterlatte.saksbehandler

import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.util.pipeline.PipelineContext
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.ktor.brukerTokenInfo
import org.slf4j.LoggerFactory

val ENHET_PATH_PARAMETER = "enhet"
inline val PipelineContext<*, ApplicationCall>.enhet: String
    get() =
        call.parameters[ENHET_PATH_PARAMETER] ?: throw NullPointerException(
            "Enhet er ikke i query params",
        )

internal fun Route.saksbehandlerRoutes(saksbehandlerService: SaksbehandlerService) {
    val logger = LoggerFactory.getLogger(this::class.java)

    route("/api") {
        get("/saksbehandlere/innlogget") {
            val saksbehandler =
                inTransaction {
                    saksbehandlerService.hentKomplettSaksbehandler(brukerTokenInfo.ident())
                }
            call.respond(saksbehandler)
        }

        get("/saksbehandlere/enhet/{$ENHET_PATH_PARAMETER}") {
            val saksbehandlere =
                inTransaction {
                    saksbehandlerService.hentSaksbehandlereForEnhet(enhet)
                }
            logger.info("Henter saksbehandlere ${saksbehandlere.size} for $enhet")
            call.respond(saksbehandlere)
        }
    }
}
