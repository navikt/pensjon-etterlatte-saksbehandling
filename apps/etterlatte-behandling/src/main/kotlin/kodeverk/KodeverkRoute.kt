package no.nav.etterlatte.kodeverk

import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo

fun Route.kodeverk(kodeverkService: KodeverkService) {
    route("api/kodeverk") {
        get("/land") {
            call.respond(kodeverkService.hentAlleLand(brukerTokenInfo))
        }

        get("/land-iso2") {
            call.respond(kodeverkService.hentAlleLandISO2(brukerTokenInfo))
        }

        get("/arkivtemaer") {
            call.respond(kodeverkService.hentArkivTemaer(brukerTokenInfo))
        }
    }
}
