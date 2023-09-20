package no.nav.etterlatte.behandling.tilbakekreving

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.medBody
import no.nav.etterlatte.libs.common.tilbakekreving.Kravgrunnlag

internal fun Route.tilbakekrevingRoutes(service: TilbakekrevingService) {
    route("/tilbakekreving") {
        post("opprett/{$SAKID_CALL_PARAMETER}") {
            medBody<Kravgrunnlag> {
                inTransaction {
                    service.opprettTilbakekreving(it)
                }
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}
