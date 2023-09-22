package no.nav.etterlatte.behandling.tilbakekreving

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.TILBAKEKREVINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.medBody
import no.nav.etterlatte.libs.common.tilbakekreving.Kravgrunnlag
import no.nav.etterlatte.libs.common.tilbakekrevingId

internal fun Route.tilbakekrevingRoutes(service: TilbakekrevingService) {
    route("/api/tilbakekreving/{$TILBAKEKREVINGID_CALL_PARAMETER}") {
        get {
            val tilbakekreving = service.hentTilbakekreving(tilbakekrevingId)
            when (tilbakekreving) {
                null -> call.respond(HttpStatusCode.NotFound)
                else -> call.respond(tilbakekreving)
            }
        }
    }

    route("/tilbakekreving") {
        post {
            medBody<Kravgrunnlag> {
                try {
                    service.opprettTilbakekreving(it)
                    call.respond(HttpStatusCode.OK)
                } catch (e: KravgrunnlagHarIkkeEksisterendeSak) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        "Eksisterer ikke sak=${it.sakId.value} for kravgrunnlag=${it.kravgrunnlagId}",
                    )
                }
            }
        }
    }
}
