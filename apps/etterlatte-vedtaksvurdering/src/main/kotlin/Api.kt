package no.nav.etterlatte

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post

fun Route.Api(service: VedtaksvurderingService) {
    get("hentVilkaarsresultat/{sakId}/{behandlingId}") {
        val sakId = call.parameters["sakId"].toString()
        val behandlingId = call.parameters["behandlingId"].toString()
        val vilkaarsresultat = service.hentVilkaarsresultat(sakId, behandlingId)
        if(vilkaarsresultat == null) {
            call.response.status(HttpStatusCode.NotFound)
        } else {
            call.respond(vilkaarsresultat)
        }

    }

    get("hentBeregningsresultat/{sakId}/{behandlingId}") {
        val sakId = call.parameters["sakId"].toString()
        val behandlingId = call.parameters["behandlingId"].toString()
        val beregningsResultat = service.hentBeregningsresultat(sakId, behandlingId)
        if(beregningsResultat == null) {
            call.response.status(HttpStatusCode.NotFound)
        } else {
            call.respond(beregningsResultat)
        }

    }

    get("hentAvkortningsresultat/{sakId}/{behandlingId}") {
        val sakId = call.parameters["sakId"].toString()
        val behandlingId = call.parameters["behandlingId"].toString()
        val avkortingsResultat = service.hentAvkorting(sakId, behandlingId)
        call.respond(avkortingsResultat)
    }

    post("fattVedtak") {
        // TODO: hvordan gjøres det?
        // lagre saksbehandlerid, at vedtaket er fattet, publisere en kafka-melding. Unikt pr behandling
        // ikke flere forslag til
       val saksbehandlerId = "AB234567"


    }
}