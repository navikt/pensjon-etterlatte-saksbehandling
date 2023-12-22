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
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.behandlingId
import no.nav.etterlatte.libs.common.sakId
import no.nav.etterlatte.libs.ktor.brukerTokenInfo
import no.nav.etterlatte.sak.TilgangService
import no.nav.etterlatte.tilgangsstyring.TILGANG_ROUTE_PATH
import no.nav.etterlatte.token.BrukerTokenInfo
import no.nav.etterlatte.token.Saksbehandler
import no.nav.etterlatte.token.Systembruker

const val SKRIVETILGANG_CALL_PARAMETER = "skrivetilgang"
inline val PipelineContext<*, ApplicationCall>.berOmSkrivetilgang: Boolean
    get() =
        call.parameters[SKRIVETILGANG_CALL_PARAMETER]?.let { it.toBoolean() } ?: throw NullPointerException(
            "Skrivetilgangparameter er ikke i path params",
        )

internal fun Route.tilgangRoutes(tilgangService: TilgangService) {
    route("/$TILGANG_ROUTE_PATH") {
        post("/person") {
            val fnr = call.receive<String>()
            val harTilgang =
                harTilgangBrukertypeSjekk(brukerTokenInfo) { _ ->
                    inTransaction {
                        tilgangService.harTilgangTilPerson(
                            fnr,
                            Kontekst.get().appUserAsSaksbehandler().saksbehandlerMedRoller,
                        )
                    }
                }
            if (berOmSkrivetilgang) {
                call.respond(harTilgang && Kontekst.get().AppUser.harSkrivetilgang())
            }
            call.respond(harTilgang)
        }

        get("/behandling/{$BEHANDLINGID_CALL_PARAMETER}/{$SKRIVETILGANG_CALL_PARAMETER}") {
            val harTilgang =
                harTilgangBrukertypeSjekk(brukerTokenInfo) { _ ->
                    tilgangService.harTilgangTilBehandling(
                        behandlingId.toString(),
                        Kontekst.get().appUserAsSaksbehandler().saksbehandlerMedRoller,
                    )
                }
            if (berOmSkrivetilgang) {
                call.respond(harTilgang && Kontekst.get().AppUser.harSkrivetilgang())
            }
            call.respond(harTilgang)
        }

        get("/sak/{$SAKID_CALL_PARAMETER}") {
            val harTilgang =
                harTilgangBrukertypeSjekk(brukerTokenInfo) { _ ->
                    tilgangService.harTilgangTilSak(
                        sakId,
                        Kontekst.get().appUserAsSaksbehandler().saksbehandlerMedRoller,
                    )
                }
            if (berOmSkrivetilgang) {
                call.respond(harTilgang && Kontekst.get().AppUser.harSkrivetilgang())
            }
            call.respond(harTilgang)
        }
    }
}

fun harTilgangBrukertypeSjekk(
    brukerTokenInfo: BrukerTokenInfo,
    harTilgang: (saksbehandler: Saksbehandler) -> Boolean,
): Boolean {
    return when (brukerTokenInfo) {
        is Saksbehandler -> {
            harTilgang(brukerTokenInfo)
        }
        is Systembruker -> true
    }
}
