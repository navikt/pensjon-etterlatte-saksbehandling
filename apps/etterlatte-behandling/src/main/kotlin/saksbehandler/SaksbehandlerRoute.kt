package no.nav.etterlatte.saksbehandler

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.util.pipeline.PipelineContext
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import org.slf4j.LoggerFactory

inline val PipelineContext<*, ApplicationCall>.enheter: List<Enhetsnummer>
    get() =
        call.request.queryParameters["enheter"]
            ?.split(",")
            ?.map { Enhetsnummer(it) } ?: emptyList()

const val IDENT = "ident"

internal fun Route.saksbehandlerRoutes(saksbehandlerService: SaksbehandlerService) {
    val logger = LoggerFactory.getLogger(this::class.java)

    route("/api") {
        get("/saksbehandlere") {
            val saksbehandlere =
                inTransaction {
                    saksbehandlerService.hentSaksbehandlereForEnhet(enheter)
                }

            logger.info("Henter saksbehandlere ${saksbehandlere.size} for $enheter")
            call.respond(saksbehandlere)
        }

        get("/saksbehandlere/innlogget") {
            val saksbehandler =
                inTransaction {
                    saksbehandlerService.hentKomplettSaksbehandler(brukerTokenInfo.ident())
                }
            call.respond(saksbehandler)
        }

        get("/saksbehandlere/navnforident/{$IDENT}") {
            val ident = call.parameters[IDENT]!!
            val navnForIdent = inTransaction { saksbehandlerService.hentNavnForIdent(ident) } ?: call.respond(HttpStatusCode.NoContent)
            call.respond(navnForIdent)
        }
    }
}
