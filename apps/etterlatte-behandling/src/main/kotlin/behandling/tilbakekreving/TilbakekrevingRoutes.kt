package no.nav.etterlatte.behandling.tilbakekreving

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.tilbakekreving.Kravgrunnlag
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingPeriode
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingVurdering
import no.nav.etterlatte.libs.ktor.route.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.TILBAKEKREVINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.kunSystembruker
import no.nav.etterlatte.libs.ktor.route.medBody
import no.nav.etterlatte.libs.ktor.route.sakId
import no.nav.etterlatte.libs.ktor.route.tilbakekrevingId
import no.nav.etterlatte.tilgangsstyring.kunSaksbehandlerMedSkrivetilgang

enum class TilbakekrevingToggles(
    private val toggle: String,
) : FeatureToggle {
    OMGJOER("omgjoer-tilbakekreving"),
    ;

    override fun key(): String = toggle
}

internal fun Route.tilbakekrevingRoutes(
    service: TilbakekrevingService,
    featureToggleService: FeatureToggleService,
) {
    route("/api/tilbakekreving") {
        route("{$TILBAKEKREVINGID_CALL_PARAMETER}") {
            get {
                call.respond(service.hentTilbakekreving(tilbakekrevingId))
            }
            put("/vurdering") {
                kunSaksbehandlerMedSkrivetilgang {
                    val vurdering = call.receive<TilbakekrevingVurdering>()
                    call.respond(service.lagreVurdering(tilbakekrevingId, vurdering, it))
                }
            }
            put("/perioder") {
                kunSaksbehandlerMedSkrivetilgang {
                    val request = call.receive<TilbakekrevingPerioderRequest>()
                    call.respond(service.lagrePerioder(tilbakekrevingId, request.perioder, it))
                }
            }
            put("/oppdater-kravgrunnlag") {
                kunSaksbehandlerMedSkrivetilgang {
                    call.respond(service.oppdaterKravgrunnlag(tilbakekrevingId, it))
                }
            }
            put("/skal-sende-brev") {
                kunSaksbehandlerMedSkrivetilgang {
                    val request = call.receive<TilbakekrevingSendeBrevRequest>()
                    call.respond(service.lagreSkalSendeBrev(tilbakekrevingId, request.skalSendeBrev, it))
                }
            }
            post("/valider") {
                kunSaksbehandlerMedSkrivetilgang {
                    call.respond(service.validerVurderingOgPerioder(tilbakekrevingId, it))
                }
            }

            post("/omgjoer") {
                kunSaksbehandlerMedSkrivetilgang {
                    if (featureToggleService.isEnabled(TilbakekrevingToggles.OMGJOER, false)) {
                        val kravgrunnlagForOmgjoering = service.hentKravgrunnlagForOmgjoering(tilbakekrevingId)
                        val omgjoering = service.opprettTilbakekreving(kravgrunnlagForOmgjoering, tilbakekrevingId)
                        call.respond(omgjoering)
                    } else {
                        throw IkkeTillattException(
                            "OMGJOERING_IKKE_ENABLED",
                            "Det er ikke skrudd på å kunne omgjøre tilbakekrevinger",
                        )
                    }
                }
            }

            route("vedtak") {
                post("fatt") {
                    kunSaksbehandlerMedSkrivetilgang {
                        service.fattVedtak(tilbakekrevingId, it)
                        call.respond(HttpStatusCode.OK)
                    }
                }
                post("attester") {
                    kunSaksbehandlerMedSkrivetilgang {
                        val (kommentar) = call.receive<TilbakekrevingAttesterRequest>()
                        service.attesterVedtak(tilbakekrevingId, kommentar, it)
                        call.respond(HttpStatusCode.OK)
                    }
                }
                post("underkjenn") {
                    kunSaksbehandlerMedSkrivetilgang {
                        val (kommentar, valgtBegrunnelse) = call.receive<TilbakekrevingUnderkjennRequest>()
                        service.underkjennVedtak(tilbakekrevingId, kommentar, valgtBegrunnelse, it)
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
                        val tilbakekreving = service.opprettTilbakekreving(kravgrunnlag = it, omgjoeringAvId = null)
                        call.respond(HttpStatusCode.OK, tilbakekreving)
                    } catch (e: TilbakekrevingHarMangelException) {
                        throw IkkeFunnetException(
                            "MANGLER_SAK",
                            "Eksisterer ikke sak=${it.sakId.value} for kravgrunnlag=${it.kravgrunnlagId}",
                            cause = e,
                        )
                    }
                }
            }
        }

        put("/oppgave-status") {
            kunSystembruker {
                medBody<OppgaveStatusRequest> {
                    service.endreTilbakekrevingOppgaveStatus(sakId, it.paaVent)
                    call.respond(HttpStatusCode.OK)
                }
            }
        }

        put("/avbryt") {
            kunSystembruker {
                medBody<AvbrytRequest> {
                    service.avbrytTilbakekreving(sakId, it.merknad)
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}

data class OppgaveStatusRequest(
    val paaVent: Boolean,
)

data class AvbrytRequest(
    val merknad: String,
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
