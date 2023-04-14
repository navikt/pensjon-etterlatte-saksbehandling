package no.nav.etterlatte.behandling.omregning

import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.behandling.Omregningshendelse
import no.nav.etterlatte.libs.ktor.bruker
import java.util.*

fun Route.omregningRoutes(
    omregningService: OmregningService
) {
    route("/omregning") {
        post {
            val request = call.receive<Omregningshendelse>()
            val (behandlingId, forrigeBehandlingId) = omregningService.opprettOmregning(
                sakId = request.sakId,
                fraDato = request.fradato,
                prosessType = request.prosesstype,
                bruker
            )
            call.respond(OpprettOmregningResponse(behandlingId, forrigeBehandlingId))
        }
    }
}

data class OpprettOmregningResponse(val behandlingId: UUID, val forrigeBehandlingId: UUID)