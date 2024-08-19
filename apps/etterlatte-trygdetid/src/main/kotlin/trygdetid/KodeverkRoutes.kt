package no.nav.etterlatte.trygdetid

import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo

fun Route.kodeverk(kodeverkService: KodeverkService) {
    route("api/trygdetid/kodeverk/land") {
        get {
            call.respond(kodeverkService.hentAlleLand(brukerTokenInfo))
        }
    }
}
