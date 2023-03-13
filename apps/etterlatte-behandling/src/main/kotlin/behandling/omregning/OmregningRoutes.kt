package no.nav.etterlatte.behandling.omregning

import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.behandling.Omregningshendelse
import java.util.*

fun Route.omregningRoutes(
    omregningService: OmregningService
) {
    route("/omregning") {
        post {
            val request = call.receive<Omregningshendelse>()
            val (behandlingId, forrigebehandlingId) = omregningService.opprettOmregning(
                sakId = request.sakId,
                fradato = request.fradato,
                prosesstype = request.prosesstype
            )
            call.respond(OpprettOmregningResponse(behandlingId, forrigebehandlingId))
        }
    }
}

data class OpprettOmregningResponse(val behandlingId: UUID, val forrigeBehandlingId: UUID)