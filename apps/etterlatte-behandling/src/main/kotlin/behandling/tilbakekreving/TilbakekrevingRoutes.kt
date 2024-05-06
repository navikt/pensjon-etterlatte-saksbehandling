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
import no.nav.etterlatte.libs.ktor.route.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.behandlingId
import no.nav.etterlatte.libs.ktor.route.kunSystembruker
import no.nav.etterlatte.libs.ktor.route.medBody
import no.nav.etterlatte.libs.ktor.route.sakId
import no.nav.etterlatte.tilgangsstyring.kunSaksbehandlerMedSkrivetilgang

internal fun Route.tilbakekrevingRoutes(service: TilbakekrevingService) {
    route("/api/tilbakekreving") {
        route("{$BEHANDLINGID_CALL_PARAMETER}") {
            get {
                call.respond(service.hentTilbakekreving(behandlingId))
            }
            put("/vurdering") {
                kunSaksbehandlerMedSkrivetilgang {
                    val vurdering = call.receive<TilbakekrevingVurdering>()
                    call.respond(service.lagreVurdering(behandlingId, vurdering, it))
                }
            }
            put("/perioder") {
                kunSaksbehandlerMedSkrivetilgang {
                    val request = call.receive<TilbakekrevingPerioderRequest>()
                    call.respond(service.lagrePerioder(behandlingId, request.perioder, it))
                }
            }
            put("/skal-sende-brev") {
                kunSaksbehandlerMedSkrivetilgang {
                    val request = call.receive<TilbakekrevingSendeBrevRequest>()
                    call.respond(service.lagreSkalSendeBrev(behandlingId, request.skalSendeBrev, it))
                }
            }
            post("/valider") {
                kunSaksbehandlerMedSkrivetilgang {
                    call.respond(service.validerVurderingOgPerioder(behandlingId, it))
                }
            }

            route("vedtak") {
                post("fatt") {
                    kunSaksbehandlerMedSkrivetilgang {
                        service.fattVedtak(behandlingId, it)
                        call.respond(HttpStatusCode.OK)
                    }
                }
                post("attester") {
                    kunSaksbehandlerMedSkrivetilgang {
                        val (kommentar) = call.receive<TilbakekrevingAttesterRequest>()
                        service.attesterVedtak(behandlingId, kommentar, it)
                        call.respond(HttpStatusCode.OK)
                    }
                }
                post("underkjenn") {
                    kunSaksbehandlerMedSkrivetilgang {
                        val (kommentar, valgtBegrunnelse) = call.receive<TilbakekrevingUnderkjennRequest>()
                        service.underkjennVedtak(behandlingId, kommentar, valgtBegrunnelse, it)
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

    route("/tilbakekreving/{$SAKID_CALL_PARAMETER}") {
        post {
            kunSystembruker {
                medBody<Kravgrunnlag> {
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

        put("/oppgave-status") {
            kunSystembruker {
                medBody<OppgaveStatusRequest> {
                    val sakId = requireNotNull(call.parameters["sakId"]).toLong()
                    service.endreTilbakekrevingOppgaveStatus(sakId, it.paaVent)
                    call.respond(HttpStatusCode.OK)
                }
            }
        }

        put("/avbryt") {
            kunSystembruker {
                val sakId = requireNotNull(call.parameters["sakId"]).toLong()
                service.avbrytTilbakekreving(sakId)
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}

data class OppgaveStatusRequest(
    val paaVent: Boolean,
)

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
