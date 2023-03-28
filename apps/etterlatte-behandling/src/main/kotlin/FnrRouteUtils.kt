package no.nav.etterlatte

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.util.pipeline.PipelineContext
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.ktor.bruker
import no.nav.etterlatte.token.Saksbehandler
import no.nav.etterlatte.token.SystemBruker

suspend inline fun PipelineContext<*, ApplicationCall>.withFoedselsnummerBehandling(
    fnr: String,
    sjekkOmFnrPaaSakHarAdresseBeskyttelse: (fnr: String) -> Boolean = { false },
    onSuccess: (fnr: Foedselsnummer) -> Unit
) = Foedselsnummer.of(fnr).let { foedselsnummer ->
    when (bruker) {
        is Saksbehandler -> {
            val harAdressebeskyttelse = sjekkOmFnrPaaSakHarAdresseBeskyttelse(fnr)
            if (harAdressebeskyttelse) {
                call.respond(HttpStatusCode.NotFound)
            } else {
                onSuccess(foedselsnummer)
            }
        }
        else -> onSuccess(foedselsnummer)
    }
}

suspend inline fun PipelineContext<*, ApplicationCall>.KunSystembruker(
    onSuccess: () -> Unit
) {
    when (bruker) {
        is SystemBruker -> {
            onSuccess()
        }
        else -> call.respond(HttpStatusCode.NotFound)
    }
}