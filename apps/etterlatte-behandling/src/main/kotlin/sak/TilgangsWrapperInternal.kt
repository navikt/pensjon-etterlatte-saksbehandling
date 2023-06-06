package no.nav.etterlatte.sak

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.util.pipeline.PipelineContext
import no.nav.etterlatte.SaksbehandlerMedRoller
import no.nav.etterlatte.libs.common.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.bruker
import no.nav.etterlatte.token.Saksbehandler

suspend inline fun PipelineContext<*, ApplicationCall>.withSakIdInternal(
    tilgangService: TilgangService,
    onSuccess: (id: Long) -> Unit
) = call.parameters[SAKID_CALL_PARAMETER]!!.toLong().let { sakId ->
    when (bruker) {
        is Saksbehandler -> {
            val harTilgangTilSak = tilgangService.harTilgangTilSak(
                sakId,
                SaksbehandlerMedRoller(bruker as Saksbehandler)
            )
            if (harTilgangTilSak) {
                onSuccess(sakId)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
        else -> onSuccess(sakId)
    }
}