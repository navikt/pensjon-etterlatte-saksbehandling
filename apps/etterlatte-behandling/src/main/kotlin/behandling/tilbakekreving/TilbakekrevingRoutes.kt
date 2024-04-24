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
import no.nav.etterlatte.libs.common.tilbakekreving.Kravgrunnlag
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingPeriode
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingVurdering
import no.nav.etterlatte.libs.ktor.brukerTokenInfo
import no.nav.etterlatte.libs.ktor.route.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.behandlingId
import no.nav.etterlatte.libs.ktor.route.medBody
import no.nav.etterlatte.libs.ktor.route.sakId
import no.nav.etterlatte.tilgangsstyring.kunSkrivetilgang

internal fun Route.tilbakekrevingRoutes(service: TilbakekrevingService) {
    route("/api/tilbakekreving") {
        route("{$BEHANDLINGID_CALL_PARAMETER}") {
            get {
                call.respond(service.hentTilbakekreving(behandlingId))
            }
            put("/vurdering") {
                kunSkrivetilgang {
                    val vurdering = call.receive<TilbakekrevingVurdering>()
                    call.respond(service.lagreVurdering(behandlingId, vurdering, brukerTokenInfo))
                }
            }
            put("/perioder") {
                kunSkrivetilgang {
                    val request = call.receive<TilbakekrevingPerioderRequest>()
                    call.respond(service.lagrePerioder(behandlingId, request.perioder, brukerTokenInfo))
                }
            }
            put("/skal-sende-brev") {
                kunSkrivetilgang {
                    val request = call.receive<TilbakekrevingSendeBrevRequest>()
                    call.respond(service.lagreSkalSendeBrev(behandlingId, request.skalSendeBrev, brukerTokenInfo))
                }
            }
            post("/valider") {
                kunSkrivetilgang {
                    call.respond(service.validerVurderingOgPerioder(behandlingId, brukerTokenInfo))
                }
            }

            route("vedtak") {
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

        get("/sak/{$SAKID_CALL_PARAMETER}") {
            val tilbakekrevinger = service.hentTilbakekrevinger(sakId)
            call.respond(tilbakekrevinger)
        }
    }

    route("/tilbakekreving") {
        post {
            medBody<Kravgrunnlag> {
                kunSkrivetilgang(sakId = it.sakId.value) {
                    try {
                        val tilbakekreving = service.opprettTilbakekreving(it)
                        call.respond(HttpStatusCode.OK, tilbakekreving)
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

data class TilbakekrevingSendeBrevRequest(
    val skalSendeBrev: Boolean,
)

data class TilbakekrevingPerioderRequest(
    val perioder: List<TilbakekrevingPeriode>,
)

data class TilbakekrevingAttesterRequest(
    val kommentar: String,
)

data class TilbakekrevingUnderkjennRequest(
    val kommentar: String,
    val valgtBegrunnelse: String,
)
