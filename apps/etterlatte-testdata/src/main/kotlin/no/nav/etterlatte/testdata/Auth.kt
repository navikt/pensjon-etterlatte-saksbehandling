package no.nav.etterlatte.testdata

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext
import no.nav.etterlatte.libs.ktor.token.Saksbehandler
import no.nav.etterlatte.libs.ktor.token.Systembruker
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo

object ADGruppe {
    const val ETTERLATTE_UTVIKLING = "650684ff-8107-4ae4-98fc-e18b5cf3188b"
    const val ETTERLATTE = "1a424f32-16a4-4b97-9d77-3e9e781a887e"
}

suspend inline fun RoutingContext.kunEtterlatte(onSuccess: () -> Unit) {
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

fun RoutingContext.harGyldigAdGruppe(): Boolean =
    when (brukerTokenInfo) {
        is Saksbehandler -> (brukerTokenInfo as Saksbehandler).groups
        is Systembruker -> (brukerTokenInfo as Systembruker).roller
    }.any { it == ADGruppe.ETTERLATTE_UTVIKLING || it == ADGruppe.ETTERLATTE }
