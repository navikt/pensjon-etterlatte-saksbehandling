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
import no.nav.etterlatte.libs.common.behandling.Grunnlagsendringshendelse
import no.nav.etterlatte.libs.common.pdlhendelse.Doedshendelse
import no.nav.etterlatte.libs.common.pdlhendelse.ForelderBarnRelasjonHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.UtflyttingsHendelse

fun Route.grunnlagsendringshendelseRoute(
    grunnlagsendringshendelseService: GrunnlagsendringshendelseService
) {
    val logger = application.log

    route("/grunnlagsendringshendelse") {
        post("/doedshendelse") {
            val doedshendelse = call.receive<Doedshendelse>()
            logger.info("Mottar en doedshendelse fra PDL")
            grunnlagsendringshendelseService.opprettSoekerDoedHendelse(doedshendelse)
            call.respond(HttpStatusCode.OK)
        }

        post("/utflyttingshendelse") {
            val utflyttingsHendelse = call.receive<UtflyttingsHendelse>()
            logger.info("Mottar en utflyttingshendelse fra PDL")
            grunnlagsendringshendelseService.opprettUtflyttingshendelse(utflyttingsHendelse)
            call.respond(HttpStatusCode.OK)
        }

        post("/forelderbarnrelasjonhendelse") {
            val forelderBarnRelasjonHendelse = call.receive<ForelderBarnRelasjonHendelse>()
            logger.info("Mottar en forelder-barn-relasjon-hendelse fra PDL")
            grunnlagsendringshendelseService.opprettForelderBarnRelasjonHendelse(forelderBarnRelasjonHendelse)
            call.respond(HttpStatusCode.OK)
        }

        route("/{sakid}") {
            get {
                call.respond(GrunnlagsendringsListe(grunnlagsendringshendelseService.hentAlleHendelserForSak(sakId)))
            }

            get("/gyldigehendelser") {
                call.respond(grunnlagsendringshendelseService.hentGyldigeHendelserForSak(sakId))
            }
        }
    }
}

data class GrunnlagsendringsListe(val hendelser: List<Grunnlagsendringshendelse>)

inline val PipelineContext<*, ApplicationCall>.sakId get() = requireNotNull(call.parameters["sakid"]).toLong()