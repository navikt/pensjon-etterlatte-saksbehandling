package no.nav.etterlatte.behandling.omregning

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.ktor.route.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.behandlingId
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import no.nav.etterlatte.tilgangsstyring.kunSkrivetilgang

fun Route.migreringRoutes(migreringService: MigreringService) {
    route("/migrering") {
        put("/{$BEHANDLINGID_CALL_PARAMETER}/avbryt") {
            kunSkrivetilgang {
                inTransaction { migreringService.avbrytBehandling(behandlingId, brukerTokenInfo) }
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}
