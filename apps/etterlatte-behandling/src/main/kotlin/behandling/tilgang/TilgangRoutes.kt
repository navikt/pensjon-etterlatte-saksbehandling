package no.nav.etterlatte.behandling.tilgang

import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.SaksbehandlerMedRoller
import no.nav.etterlatte.TILGANG_ROUTE_PATH
import no.nav.etterlatte.libs.common.BEHANDLINGSID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.behandlingsId
import no.nav.etterlatte.libs.common.sakId
import no.nav.etterlatte.libs.ktor.bruker
import no.nav.etterlatte.sak.TilgangService
import no.nav.etterlatte.token.Bruker
import no.nav.etterlatte.token.Saksbehandler
import no.nav.etterlatte.token.SystemBruker

internal fun Route.tilgangRoutes(tilgangService: TilgangService) {
    route("/$TILGANG_ROUTE_PATH") {
        post("/person") {
            val fnr = call.receive<String>()

            val harTilgang = harTilgangBrukertypeSjekk(
                bruker = bruker,
                harTilgang = {
                    tilgangService.harTilgangTilPerson(
                        fnr,
                        SaksbehandlerMedRoller(bruker)
                    )
                }
            )
            call.respond(harTilgang)
        }

        get("/behandling/{$BEHANDLINGSID_CALL_PARAMETER}") {
            val harTilgang = harTilgangBrukertypeSjekk(
                bruker = bruker,
                harTilgang = {
                    tilgangService.harTilgangTilBehandling(
                        behandlingsId.toString(),
                        SaksbehandlerMedRoller(bruker)
                    )
                }
            )

            call.respond(harTilgang)
        }

        get("/sak/{$SAKID_CALL_PARAMETER}") {
            val harTilgang = harTilgangBrukertypeSjekk(
                bruker = bruker,
                harTilgang = {
                    tilgangService.harTilgangTilSak(
                        sakId,
                        SaksbehandlerMedRoller(bruker)
                    )
                }
            )
            call.respond(harTilgang)
        }
    }
}

fun harTilgangBrukertypeSjekk(bruker: Bruker, harTilgang: () -> Boolean): Boolean {
    return when (bruker) {
        is Saksbehandler -> {
            harTilgang()
        }
        is SystemBruker -> true
    }
}