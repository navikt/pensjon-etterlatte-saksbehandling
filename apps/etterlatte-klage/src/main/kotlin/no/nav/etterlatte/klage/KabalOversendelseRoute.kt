package no.nav.etterlatte.klage

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.behandling.KlageOversendelseDto
import no.nav.etterlatte.libs.ktor.route.kunSystembruker
import no.nav.etterlatte.libs.ktor.route.medBody

fun Route.kabalOvesendelseRoute(kabalOversendelseService: KabalOversendelseService) {
    route("api") {
        route("send-klage") {
            post {
                kunSystembruker {
                    medBody<KlageOversendelseDto> {
                        kabalOversendelseService.sendTilKabal(it.klage, it.ekstraData)
                        call.respond(HttpStatusCode.OK)
                    }
                }
            }
        }
    }
}
