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
import no.nav.etterlatte.libs.common.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.behandlingId
import no.nav.etterlatte.libs.common.medBody
import no.nav.etterlatte.libs.common.tilbakekreving.Kravgrunnlag
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingPeriode
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingVurdering
import no.nav.etterlatte.libs.ktor.brukerTokenInfo
import no.nav.etterlatte.tilgangsstyring.kunSkrivetilgang

internal fun Route.tilbakekrevingRoutes(service: TilbakekrevingService) {
    route("/api/tilbakekreving/{$BEHANDLINGID_CALL_PARAMETER}") {
        get {
            try {
                call.respond(service.hentTilbakekreving(behandlingId))
            } catch (e: TilbakekrevingFinnesIkkeException) {
                call.respond(HttpStatusCode.NotFound)
            }
        }
        put("/vurdering") {
            kunSkrivetilgang {
                val vurdering = call.receive<TilbakekrevingVurdering>()
                try {
                    call.respond(service.lagreVurdering(behandlingId, vurdering))
                } catch (e: TilbakekrevingFinnesIkkeException) {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
        put("/perioder") {
            kunSkrivetilgang {
                val request = call.receive<TilbakekrevingLagreRequest>()
                try {
                    call.respond(service.lagrePerioder(behandlingId, request.perioder))
                } catch (e: TilbakekrevingFinnesIkkeException) {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }

        route("vedtak") {
            post("opprett") {
                kunSkrivetilgang {
                    service.opprettVedtak(behandlingId, brukerTokenInfo)
                    call.respond(HttpStatusCode.OK)
                }
            }
            post("fatt") {
                kunSkrivetilgang {
                    service.fattVedtak(behandlingId, brukerTokenInfo)
                    call.respond(HttpStatusCode.OK)
                }
            }
            post("attester") {
                kunSkrivetilgang {
                    val (kommentar) = call.receive<TilbakekrevingAttesterRequest>()
                    service.attesterVedtak(behandlingId, kommentar, brukerTokenInfo)
                    call.respond(HttpStatusCode.OK)
                }
            }
            post("underkjenn") {
                kunSkrivetilgang {
                    val (kommentar, valgtBegrunnelse) = call.receive<TilbakekrevingUnderkjennRequest>()
                    service.underkjennVedtak(behandlingId, kommentar, valgtBegrunnelse, brukerTokenInfo)
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }

    route("/tilbakekreving") {
        post {
            kunSkrivetilgang {
                medBody<Kravgrunnlag> {
                    try {
                        service.opprettTilbakekreving(it)
                        call.respond(HttpStatusCode.OK)
                    } catch (e: TilbakekrevingHarMangelException) {
                        call.respond(
                            HttpStatusCode.NotFound,
                            "Eksisterer ikke sak=${it.sakId.value} for kravgrunnlag=${it.kravgrunnlagId}",
                        )
                    }
                }
            }
        }
    }
}

data class TilbakekrevingLagreRequest(
    val perioder: List<TilbakekrevingPeriode>,
)

data class TilbakekrevingAttesterRequest(
    val kommentar: String,
)

data class TilbakekrevingUnderkjennRequest(
    val kommentar: String,
    val valgtBegrunnelse: String,
)
