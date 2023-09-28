package no.nav.etterlatte.behandling.tilbakekreving

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.TILBAKEKREVINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.medBody
import no.nav.etterlatte.libs.common.tilbakekreving.Kravgrunnlag
import no.nav.etterlatte.libs.common.tilbakekrevingId

internal fun Route.tilbakekrevingRoutes(service: TilbakekrevingService) {
    route("/api/tilbakekreving/{$TILBAKEKREVINGID_CALL_PARAMETER}") {
        get {
            try {
                call.respond(service.hentTilbakekreving(tilbakekrevingId))
            } catch (e: TilbakekrevingFinnesIkkeException) {
                call.respond(HttpStatusCode.NotFound)
            }
        }
        put("/vurdering") {
            val vurdering = call.receive<TilbakekrevingVurdering>()
            try {
                call.respond(service.lagreVurdering(tilbakekrevingId, vurdering))
            } catch (e: TilbakekrevingFinnesIkkeException) {
                call.respond(HttpStatusCode.NotFound)
            }
        }
        put("/perioder") {
            val request = call.receive<TilbakekrevingLagreRequest>()
            try {
                call.respond(service.lagrePerioder(tilbakekrevingId, request.perioder))
            } catch (e: TilbakekrevingFinnesIkkeException) {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }

    route("/tilbakekreving") {
        post {
            medBody<Kravgrunnlag> {
                try {
                    service.opprettTilbakekreving(it)
                    call.respond(HttpStatusCode.OK)
                } catch (e: KravgrunnlagHarIkkeEksisterendeSakException) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        "Eksisterer ikke sak=${it.sakId.value} for kravgrunnlag=${it.kravgrunnlagId}",
                    )
                }
            }
        }
    }
}

data class TilbakekrevingLagreRequest(
    val perioder: List<TilbakekrevingPeriode>,
)
