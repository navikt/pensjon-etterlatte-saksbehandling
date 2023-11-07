package no.nav.etterlatte.behandling.bosattutland

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.behandlingId
import no.nav.etterlatte.libs.common.kunSaksbehandler

internal fun Route.bosattUtlandRoutes(bosattUtlandService: BosattUtlandService) {
    val logger = application.log
    route("api/bosattutland/{$BEHANDLINGID_CALL_PARAMETER}") {
        post {
            kunSaksbehandler {
                val request = call.receive<BosattUtland>()
                val bosattUtland = bosattUtlandService.lagreBosattUtland(request)
                call.respond(bosattUtland)
            }
        }
        get {
            kunSaksbehandler {
                when (val bosattUtland = bosattUtlandService.hentBosattUtland(behandlingId)) {
                    null -> call.respond(HttpStatusCode.NotFound)
                    else -> call.respond(bosattUtland)
                }
            }
        }
    }
}
