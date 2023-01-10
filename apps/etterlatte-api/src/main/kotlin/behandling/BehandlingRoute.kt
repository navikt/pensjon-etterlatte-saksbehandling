package no.nav.etterlatte

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.libs.common.person.InvalidFoedselsnummer
import no.nav.etterlatte.libs.ktor.saksbehandler
import org.slf4j.LoggerFactory
import java.util.*

fun Route.behandlingRoute(service: BehandlingService) {
    val logger = LoggerFactory.getLogger(this::class.java)

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
        }
    }

    route("behandling/{behandlingId}") {
        get {
            call.withUUID("behandlingId") {
                call.respond(service.hentBehandling(it.toString(), getAccessToken(call)))
            }
        }

        get("hendelser") {
            call.withUUID("behandlingId") {
                call.respond(service.hentHendelserForBehandling(it.toString(), getAccessToken(call)))
            }
        }
    }
}

suspend fun ApplicationCall.withUUID(parameter: String, onSuccess: (suspend (id: UUID) -> Unit)) {
    val id = this.parameters[parameter]
    if (id == null) {
        this.respond(HttpStatusCode.BadRequest, "Fant ikke følgende parameter: $parameter")
    }

    try {
        onSuccess(UUID.fromString(id))
    } catch (e: IllegalArgumentException) {
        this.respond(HttpStatusCode.BadRequest, "Ikke ett gyldigt UUID-format")
    }
}