package no.nav.etterlatte.behandling.forlengetoms

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.ktor.route.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.behandlingId
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import no.nav.etterlatte.tilgangsstyring.kunSaksbehandlerMedSkrivetilgang

internal fun Route.forlengetOmsRoutes(service: ForlengetOmsService) {
    route("/api/behandling/{$BEHANDLINGID_CALL_PARAMETER}/forlenget-oms") {
        get {
            val aktivitetOgUtfall = inTransaction { service.hentAktivitetOgUtfall(behandlingId) }
            if (aktivitetOgUtfall == null) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(aktivitetOgUtfall)
            }
        }
        post {
            kunSaksbehandlerMedSkrivetilgang {
                val request = call.receive<LagreForlengetOmsVurdering>()
                val vurdering = inTransaction { service.lagreVurdering(behandlingId, request, brukerTokenInfo) }
                call.respond(vurdering)
            }
        }
    }
}
