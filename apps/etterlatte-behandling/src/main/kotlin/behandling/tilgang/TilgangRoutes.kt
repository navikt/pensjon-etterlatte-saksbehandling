package no.nav.etterlatte.behandling.tilgang

import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.util.pipeline.PipelineContext
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.libs.ktor.route.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.behandlingId
import no.nav.etterlatte.libs.ktor.route.sakId
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Saksbehandler
import no.nav.etterlatte.libs.ktor.token.Systembruker
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import no.nav.etterlatte.sak.TilgangService
import no.nav.etterlatte.tilgangsstyring.TILGANG_ROUTE_PATH
import no.nav.etterlatte.tilgangsstyring.sjekkSkrivetilgang

const val SKRIVETILGANG_CALL_QUERYPARAMETER = "skrivetilgang"
inline val PipelineContext<*, ApplicationCall>.berOmSkrivetilgang: Boolean
    get() =
        call.request.queryParameters[SKRIVETILGANG_CALL_QUERYPARAMETER]?.toBoolean() ?: throw NullPointerException(
            "Skrivetilgangparameter er ikke i query params",
        )

internal fun Route.tilgangRoutes(tilgangService: TilgangService) {
    route("/$TILGANG_ROUTE_PATH") {
        post("/person") {
            val fnr = call.receive<String>()
            val harTilgang =
                harTilgangBrukertypeSjekk(brukerTokenInfo) { _ ->
                    tilgangService.harTilgangTilPerson(
                        fnr,
                        Kontekst.get().appUserAsSaksbehandler().saksbehandlerMedRoller,
                    )
                }
            call.respond(harTilgang)
        }

        get("/behandling/{$BEHANDLINGID_CALL_PARAMETER}") {
            if (berOmSkrivetilgang && !sjekkSkrivetilgang()) {
                call.respond(false)
            }
            val harTilgang =
                harTilgangBrukertypeSjekk(brukerTokenInfo) { _ ->
                    tilgangService.harTilgangTilBehandling(
                        behandlingId.toString(),
                        Kontekst.get().appUserAsSaksbehandler().saksbehandlerMedRoller,
                    )
                }

            call.respond(harTilgang)
        }

        get("/sak/{$SAKID_CALL_PARAMETER}") {
            if (berOmSkrivetilgang && !sjekkSkrivetilgang()) {
                call.respond(false)
            }
            val harTilgang =
                harTilgangBrukertypeSjekk(brukerTokenInfo) { _ ->
                    tilgangService.harTilgangTilSak(
                        sakId,
                        Kontekst.get().appUserAsSaksbehandler().saksbehandlerMedRoller,
                    )
                }
            call.respond(harTilgang)
        }
    }
}

fun harTilgangBrukertypeSjekk(
    brukerTokenInfo: BrukerTokenInfo,
    harTilgang: (saksbehandler: Saksbehandler) -> Boolean,
): Boolean =
    when (brukerTokenInfo) {
        is Saksbehandler -> {
            harTilgang(brukerTokenInfo)
        }
        is Systembruker -> true
    }
