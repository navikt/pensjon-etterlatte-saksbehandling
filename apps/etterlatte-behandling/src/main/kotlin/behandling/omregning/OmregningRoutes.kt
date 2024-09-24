package no.nav.etterlatte.behandling.omregning

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.sak.KjoeringRequest
import no.nav.etterlatte.libs.common.sak.LagreKjoeringRequest
import no.nav.etterlatte.libs.ktor.route.routeLogger
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo

fun Route.omregningRoutes(omregningService: OmregningService) {
    val logger = routeLogger

    route("/omregning") {
        put("kjoering") {
            val request = call.receive<KjoeringRequest>()
            logger.info("Motter hendelse om at regulering har status ${request.status.name} i sak ${request.sakId}")
            inTransaction {
                omregningService.oppdaterKjoering(request, brukerTokenInfo)
            }
            call.respond(HttpStatusCode.OK)
        }

        post("kjoeringFullfoert") {
            val request = call.receive<LagreKjoeringRequest>()
            inTransaction {
                omregningService.kjoeringFullfoert(request)
            }
            call.respond(HttpStatusCode.Created)
        }
    }
}
