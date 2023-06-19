package no.nav.etterlatte.behandling.tilgang

import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.libs.common.BEHANDLINGSID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.behandlingsId
import no.nav.etterlatte.libs.common.sakId
import no.nav.etterlatte.libs.ktor.bruker
import no.nav.etterlatte.sak.TilgangService
import no.nav.etterlatte.token.Bruker
import no.nav.etterlatte.token.Saksbehandler
import no.nav.etterlatte.token.SystemBruker
import tilgangsstyring.TILGANG_ROUTE_PATH

internal fun Route.tilgangRoutes(tilgangService: TilgangService) {
    route("/$TILGANG_ROUTE_PATH") {
        post("/person") {
            val fnr = call.receive<String>()

            val harTilgang = harTilgangBrukertypeSjekk(bruker) { saksbehandler ->
                tilgangService.harTilgangTilPerson(
                    fnr,
                    Kontekst.get().appUserAsSaksbehandler().saksbehandlerMedRoller
                )
            }
            call.respond(harTilgang)
        }

        get("/behandling/{$BEHANDLINGSID_CALL_PARAMETER}") {
            val harTilgang = harTilgangBrukertypeSjekk(bruker) { saksbehandler ->
                tilgangService.harTilgangTilBehandling(
                    behandlingsId.toString(),
                    Kontekst.get().appUserAsSaksbehandler().saksbehandlerMedRoller
                )
            }
            call.respond(harTilgang)
        }

        get("/sak/{$SAKID_CALL_PARAMETER}") {
            val harTilgang = harTilgangBrukertypeSjekk(bruker) { saksbehandler ->
                tilgangService.harTilgangTilSak(
                    sakId,
                    Kontekst.get().appUserAsSaksbehandler().saksbehandlerMedRoller
                )
            }
            call.respond(harTilgang)
        }
    }
}

fun harTilgangBrukertypeSjekk(bruker: Bruker, harTilgang: (saksbehandler: Saksbehandler) -> Boolean): Boolean {
    return when (bruker) {
        is Saksbehandler -> {
            harTilgang(bruker)
        }
        is SystemBruker -> true
    }
}