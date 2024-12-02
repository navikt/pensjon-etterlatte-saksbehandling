package no.nav.etterlatte.no.nav.etterlatte.testdata

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.util.pipeline.PipelineContext
import no.nav.etterlatte.libs.ktor.token.Saksbehandler
import no.nav.etterlatte.libs.ktor.token.Systembruker
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo

object ADGruppe {
    const val ETTERLATTE = "650684ff-8107-4ae4-98fc-e18b5cf3188b"
}

suspend inline fun PipelineContext<*, ApplicationCall>.kunEtterlatteUtvikling(onSuccess: () -> Unit) {
    val rollerEllerAdGrupper =
        when (brukerTokenInfo) {
            is Saksbehandler -> (call.brukerTokenInfo as Saksbehandler).groups
            is Systembruker -> (call.brukerTokenInfo as Systembruker).roller
        }
    if (rollerEllerAdGrupper.any { it == ADGruppe.ETTERLATTE }) {
        onSuccess()
    } else {
        call.respond(HttpStatusCode.Unauthorized, "Mangler etterlatte-rolle")
    }
}

fun PipelineContext<*, ApplicationCall>.harGyldigAdGruppe(): Boolean =
    when (brukerTokenInfo) {
        is Saksbehandler -> (brukerTokenInfo as Saksbehandler).groups
        is Systembruker -> (brukerTokenInfo as Systembruker).roller
    }.any { it == ADGruppe.ETTERLATTE }
