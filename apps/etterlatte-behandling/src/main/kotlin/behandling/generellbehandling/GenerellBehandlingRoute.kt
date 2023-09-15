package no.nav.etterlatte.behandling.generellbehandling

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import no.nav.etterlatte.libs.common.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.kunSaksbehandler
import no.nav.etterlatte.libs.common.sakId
import no.nav.etterlatte.sak.SakService
import java.util.*

internal fun Route.generellbehandlingRoutes(
    generellBehandlingService: GenerellBehandlingService,
    sakService: SakService
) {
    val logger = application.log

    post("/api/generellbehandling/{$SAKID_CALL_PARAMETER}") {
        kunSaksbehandler {
            val request = call.receive<OpprettGenerellBehandlingRequest>()
            val finnSak = sakService.finnSak(sakId)
            if (finnSak == null) {
                call.respond(HttpStatusCode.BadRequest, "Saken finnes ikke")
            }
            val id = UUID.randomUUID()
            generellBehandlingService.opprettBehandling(
                GenerellBehandling(id, sakId, request.innhold)
            )
            logger.info(
                "Opprettet generell behandling for sak $sakId av typen ${request.innhold::class.simpleName}, id: $id"
            )
            call.respond(HttpStatusCode.OK)
        }
    }

    get("api/generellbehandling/{generellbehandlingId}") {
        kunSaksbehandler {
            val id =
                call.parameters["generellbehandlingId"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "Saken finnes ikke")
            val hentetBehandling = generellBehandlingService.hentBehandlingMedId(UUID.fromString(id))
            call.respond(hentetBehandling ?: HttpStatusCode.NotFound)
        }
    }
    get("api/generellbehandling/{$SAKID_CALL_PARAMETER}") {
        kunSaksbehandler {
            call.respond(generellBehandlingService.hentBehandlingForSak(sakId))
        }
    }
}

data class OpprettGenerellBehandlingRequest(
    val innhold: Innhold
)