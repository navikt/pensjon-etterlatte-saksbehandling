package no.nav.etterlatte

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.logger


// /api
fun Route.behandlingRoute(service: BehandlingService) {

    route("saker") {
        //hent alle saker

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
        }
    }

    route("behandling/{behandlingId}") {
        get {
            val behandlingId = call.parameters["behandlingId"]?.toString()
            if (behandlingId == null) {
                call.response.status(HttpStatusCode(400, "Bad request"))
                call.respond("BehandlingsId mangler")
            } else {
                call.respond(service.hentBehandling(behandlingId, getAccessToken(call)))
            }
        }

        get("hendelser") {
            val behandlingId = call.parameters["behandlingId"]
            if (behandlingId == null) {
                call.response.status(HttpStatusCode(400, "Bad request"))
                call.respond("BehandlingsId mangler")
            } else {
                call.respond(service.hentHendelserForBehandling(behandlingId, getAccessToken(call)))
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
                } catch (e: Exception) {
                    logger.info("Feil ved henting av person og saker med melding", e)
                    throw e
                }
            }
        }

    }

}
