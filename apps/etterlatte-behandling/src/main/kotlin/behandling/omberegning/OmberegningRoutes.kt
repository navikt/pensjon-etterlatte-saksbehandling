package no.nav.etterlatte.behandling.omberegning

import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.behandling.Omberegningshendelse

fun Route.omberegningRoutes(
    omberegningService: OmberegningService
) {
    route("/omberegning") {
        post {
            val request = call.receive<Omberegningshendelse>()
            val omberegning = omberegningService.opprettOmberegning(
                sakId = request.sakId,
                fradato = request.fradato,
                prosesstype = request.prosesstype
            )
            call.respond(omberegning)
        }
    }
}