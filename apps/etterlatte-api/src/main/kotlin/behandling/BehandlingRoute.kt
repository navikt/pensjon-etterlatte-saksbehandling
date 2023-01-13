package no.nav.etterlatte

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.etterlatte.behandling.BehandlingService
import java.util.*

fun Route.behandlingRoute(service: BehandlingService) {
    route("behandling/{behandlingId}") {
        get {
            call.withUUID("behandlingId") {
                call.respond(service.hentBehandling(it.toString(), getAccessToken(call)))
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