package no.nav.etterlatte.grunnlagsendring

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.util.pipeline.PipelineContext
import no.nav.etterlatte.libs.common.pdlhendelse.Doedshendelse

fun Route.grunnlagsendringshendelseRoute(
    grunnlagsendringshendelseService: GrunnlagsendringshendelseService
) {
    val logger = application.log

    route("/grunnlagsendringshendelse") {
        post("/doedshendelse") {
            val doedshendelse = call.receive<Doedshendelse>()
            logger.info("Mottar doedshendelse: $doedshendelse")
            grunnlagsendringshendelseService.opprettSoekerDoedHendelse(doedshendelse)
            call.respond(HttpStatusCode.OK)
        }

        get("/{sakid}/gyldigehendelser") {
            call.respond(grunnlagsendringshendelseService.hentGyldigeHendelserForSak(sakId))
        }
    }
}

inline val PipelineContext<*, ApplicationCall>.sakId get() = requireNotNull(call.parameters["sakid"]).toLong()