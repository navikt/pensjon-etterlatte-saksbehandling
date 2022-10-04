package no.nav.etterlatte

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.util.pipeline.PipelineContext
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.logger
import no.nav.etterlatte.libs.common.behandling.ManueltOpphoerRequest
import no.nav.etterlatte.libs.common.person.InvalidFoedselsnummer
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.*

val logger = LoggerFactory.getLogger("no.nav.etterlatte.behandling.BehandlingRoute")
fun Route.behandlingRoute(service: BehandlingService) {
    route("saker") {
        // hent alle sakerª
        get {
            try {
                val accessToken = getAccessToken(call)
                val list = service.hentSaker(accessToken)
                call.respond(list)
            } catch (e: Exception) {
                throw e
            }
        }

        route("{sakId}") {
            // hent spesifikk sak med tilhørende behandlinger
            get {
                val sakId = call.parameters["sakId"]?.toInt()
                if (sakId == null) {
                    call.response.status(HttpStatusCode(400, "Bad request"))
                    call.respond("SakId mangler")
                } else {
                    call.respond(service.hentBehandlingerForSak(sakId, getAccessToken(call)))
                }
            }

            // Slett alle behandlinger på en sak
            delete("behandlinger") {
                val sakId = call.parameters["sakId"]?.toInt()
                if (sakId == null) {
                    call.response.status(HttpStatusCode(400, "Bad request"))
                    call.respond("SakId mangler")
                } else {
                    if (service.slettBehandlinger(sakId, getAccessToken(call))) {
                        call.respond(HttpStatusCode.OK)
                    }
                }
            }

            delete("revurderinger") {
                val sakId = call.parameters["sakId"]?.toInt()
                if (sakId == null) {
                    call.response.status(HttpStatusCode(400, "Bad request"))
                    call.respond("SakId mangler")
                } else {
                    if (service.slettRevurderinger(sakId, getAccessToken(call))) {
                        call.respond(HttpStatusCode.OK)
                    } else {
                        call.respond(HttpStatusCode.InternalServerError)
                    }
                }
            }

            post("manueltopphoer") {
                try {
                    call.receive<ManueltOpphoerRequest>().also { req ->
                        service.opprettManueltOpphoer(req, getAccessToken(call)).also { opprettet ->
                            call.respond(opprettet)
                        }
                    }
                } catch (e: Exception) {
                    logger.error("Kunne ikke opprette manuelt opphoer", e)
                    call.respond(HttpStatusCode.InternalServerError)
                }
            }
        }
    }

    route("behandling/{behandlingId}") {
            get {
                if (call.parameters["behandlingId"] == null) {
                    call.respond("BehandlingsId mangler")
                } else {
                    call.respond(service.hentBehandling(behandlingId, getAccessToken(call)))
                }
            }

            get("hendelser") {
                if (call.parameters["behandlingId"] == null) {
                    call.respond(HttpStatusCode.BadRequest, "BehandlingsId mangler")
                } else {
                    call.respond(service.hentHendelserForBehandling(behandlingId, getAccessToken(call)))
                }
            }

            post("avbryt") {
                val behandlingId = call.parameters["behandlingId"]
                if (behandlingId == null) {
                    call.respond(HttpStatusCode.BadRequest, "BehandlingsId mangler")
                } else {
                    call.respond(service.avbrytBehanding(behandlingId, getAccessToken(call)))
                }
            }
    }

    route("personer") {
        get("{fnr}") {
            val fnr = call.parameters["fnr"]
            if (fnr == null) {
                call.response.status(HttpStatusCode(400, "Bad request"))
                call.respond("Fødselsnummer mangler")
            } else {
                try {
                    call.respond(service.hentPersonOgSaker(fnr, getAccessToken(call)))
                } catch (e: InvalidFoedselsnummer) {
                    logger.error("Ugyldig fødselsnummer", e)
                    call.respond(HttpStatusCode.BadRequest, "Ugyldig fødselsnummer")
                }
            }
        }
    }
}

inline val PipelineContext<*, ApplicationCall>.behandlingId
    get() = requireNotNull(call.parameters["behandlingId"]).let {
        UUID.fromString(
            it
        ).toString()
    }