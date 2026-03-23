package no.nav.etterlatte.vedtaksvurdering

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.ktor.route.withSakId
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import no.nav.etterlatte.vedtaksvurdering.klienter.BehandlingKlient
import java.util.UUID

data class FattVedtakCrudRequest(
    val vedtakFattet: VedtakFattet,
)

data class AttesterVedtakCrudRequest(
    val attestasjon: Attestasjon,
)

data class HentFerdigstilteVedtakRequest(
    val fnr: String,
    val sakType: SakType? = null,
)

data class HentAvkortetYtelsePerioderRequest(
    val vedtakIds: Set<Long>,
)

data class LagreManuellSamordningsmeldingCrudRequest(
    val oppdatering: OppdaterSamordningsmelding,
    val sakId: SakId,
    val saksbehandlerIdent: String,
)

data class HentSakIdMedUtbetalingRequest(
    val sakType: SakType? = null,
)

fun Route.vedtakCrudRoute(
    repository: VedtaksvurderingRepository,
    behandlingKlient: BehandlingKlient,
) {
    route("/intern/vedtak-crud") {
        post("/opprett") {
            val opprettVedtak = call.receive<OpprettVedtak>()
            val vedtak = repository.opprettVedtak(opprettVedtak)
            call.respond(vedtak)
        }

        put("/oppdater") {
            val vedtak = call.receive<Vedtak>()
            val oppdatert = repository.oppdaterVedtak(vedtak)
            call.respond(oppdatert)
        }

        get("/behandling/{behandlingId}") {
            val behandlingId = UUID.fromString(call.parameters["behandlingId"])
            val vedtak = repository.hentVedtak(behandlingId)
            if (vedtak != null) {
                call.respond(vedtak)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        get("/{vedtakId}") {
            val vedtakId = krevIkkeNull(call.parameters["vedtakId"]?.toLong()) { "vedtakId mangler" }
            val vedtak = repository.hentVedtak(vedtakId)
            if (vedtak != null) {
                call.respond(vedtak)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        get("/sak/{sakId}") {
            val sakId = SakId(krevIkkeNull(call.parameters["sakId"]?.toLong()) { "sakId mangler" })
            val vedtak = repository.hentVedtakForSak(sakId)
            call.respond(vedtak)
        }

        post("/behandling/{behandlingId}/fatt") {
            val behandlingId = UUID.fromString(call.parameters["behandlingId"])
            val request = call.receive<FattVedtakCrudRequest>()
            val vedtak = repository.fattVedtak(behandlingId = behandlingId, vedtakFattet = request.vedtakFattet)
            call.respond(vedtak)
        }

        post("/behandling/{behandlingId}/attester") {
            val behandlingId = UUID.fromString(call.parameters["behandlingId"])
            val request = call.receive<AttesterVedtakCrudRequest>()
            val vedtak = repository.attesterVedtak(behandlingId = behandlingId, attestasjon = request.attestasjon)
            call.respond(vedtak)
        }

        post("/behandling/{behandlingId}/underkjenn") {
            val behandlingId = UUID.fromString(call.parameters["behandlingId"])
            val vedtak = repository.underkjennVedtak(behandlingId)
            call.respond(vedtak)
        }

        post("/behandling/{behandlingId}/til-samordning") {
            val behandlingId = UUID.fromString(call.parameters["behandlingId"])
            val vedtak = repository.tilSamordningVedtak(behandlingId)
            call.respond(vedtak)
        }

        post("/behandling/{behandlingId}/samordnet") {
            val behandlingId = UUID.fromString(call.parameters["behandlingId"])
            val vedtak = repository.samordnetVedtak(behandlingId)
            call.respond(vedtak)
        }

        post("/behandling/{behandlingId}/iverksatt") {
            val behandlingId = UUID.fromString(call.parameters["behandlingId"])
            val vedtak = repository.iverksattVedtak(behandlingId)
            call.respond(vedtak)
        }

        patch("/behandling/{behandlingId}/tilbakestill") {
            val behandlingId = UUID.fromString(call.parameters["behandlingId"])
            val vedtak = repository.tilbakestillIkkeIverksatteVedtak(behandlingId)
            if (vedtak != null) {
                call.respond(vedtak)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        post("/ferdigstilte") {
            val request = call.receive<HentFerdigstilteVedtakRequest>()
            val fnr = Folkeregisteridentifikator.of(request.fnr)
            val vedtak = repository.hentFerdigstilteVedtak(fnr = fnr, sakType = request.sakType)
            call.respond(vedtak)
        }

        get("/sak-utbetaling/{inntektsaar}") {
            val inntektsaar = krevIkkeNull(call.parameters["inntektsaar"]?.toInt()) { "inntektsaar mangler" }
            val sakType = call.parameters["sakType"]?.let { SakType.valueOf(it) }
            val sakIder = repository.hentSakIdMedUtbetalingForInntektsaar(inntektsaar = inntektsaar, sakType = sakType)
            call.respond(sakIder)
        }

        get("/sak/{sakId}/har-utbetaling/{inntektsaar}/{sakType}") {
            val sakId = SakId(krevIkkeNull(call.parameters["sakId"]?.toLong()) { "sakId mangler" })
            val inntektsaar = krevIkkeNull(call.parameters["inntektsaar"]?.toInt()) { "inntektsaar mangler" }
            val sakType = SakType.valueOf(krevIkkeNull(call.parameters["sakType"]) { "sakType mangler" })
            val harUtbetaling = repository.harSakUtbetalingForInntektsaar(sakId = sakId, inntektsaar = inntektsaar, sakType = sakType)
            call.respond(mapOf("harUtbetaling" to harUtbetaling))
        }

        post("/avkortet-ytelse") {
            val request = call.receive<HentAvkortetYtelsePerioderRequest>()
            val perioder = repository.hentAvkortetYtelsePerioder(request.vedtakIds)
            call.respond(perioder)
        }

        post("/samordning-manuell") {
            val request = call.receive<LagreManuellSamordningsmeldingCrudRequest>()
            withSakId(
                sakId = request.sakId,
                sakTilgangsSjekk = behandlingKlient,
                skrivetilgang = true,
            ) {
                repository.lagreManuellBehandlingSamordningsmelding(
                    oppdatering = request.oppdatering,
                    brukerTokenInfo = brukerTokenInfo,
                )
            }
            call.respond(HttpStatusCode.OK)
        }

        delete("/samordning-manuell/{samId}") {
            val samId = krevIkkeNull(call.parameters["samId"]?.toLong()) { "samId mangler" }
            repository.slettManuellBehandlingSamordningsmelding(samId)
            call.respond(HttpStatusCode.OK)
        }

        post("/tilbakestill-tilbakekreving/{tilbakekrevingId}") {
            val tilbakekrevingId = UUID.fromString(call.parameters["tilbakekrevingId"])
            repository.tilbakestillTilbakekrevingsvedtak(tilbakekrevingId)
            call.respond(HttpStatusCode.OK)
        }
    }
}
