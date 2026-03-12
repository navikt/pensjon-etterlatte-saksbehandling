package no.nav.etterlatte.behandling.vedtaksvurdering.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.behandling.vedtaksvurdering.LoependeYtelse
import no.nav.etterlatte.behandling.vedtaksvurdering.service.VedtakSamordningService
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.GenerellIkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.vedtak.LoependeYtelseDTO
import no.nav.etterlatte.libs.ktor.route.FoedselsnummerDTO
import java.time.LocalDate

fun Route.samordningSystembrukerVedtakRoute(vedtakSamordningService: VedtakSamordningService) {
    route("/api/samordning/vedtak") {
        post {
            val sakstype =
                call.parameters["sakstype"]?.let { runCatching { SakType.valueOf(it) }.getOrNull() }
                    ?: return@post call.respond(HttpStatusCode.BadRequest, "sakstype ikke angitt")
            val fomDato =
                call.parameters["fomDato"]?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                    ?: return@post call.respond(HttpStatusCode.BadRequest, "fomDato ikke angitt")
            val fnr = call.receive<FoedselsnummerDTO>().foedselsnummer.let { Folkeregisteridentifikator.of(it) }

            val vedtaksliste =
                vedtakSamordningService.hentVedtaksliste(
                    fnr = fnr,
                    sakType = sakstype,
                    fomDato = fomDato,
                )
            call.respond(vedtaksliste)
        }

        get("/{vedtakId}") {
            val vedtakId =
                krevIkkeNull(call.parameters["vedtakId"]?.toLong()) {
                    "VedtakId mangler"
                }

            val vedtak =
                vedtakSamordningService.hentVedtak(vedtakId)
                    ?: throw GenerellIkkeFunnetException()
            call.respond(vedtak)
        }
    }
}

private fun LoependeYtelse.toDto() =
    LoependeYtelseDTO(
        erLoepende = erLoepende,
        underSamordning = underSamordning,
        dato = dato,
        behandlingId = behandlingId,
        sisteLoependeBehandlingId = sisteLoependeBehandlingId,
    )
