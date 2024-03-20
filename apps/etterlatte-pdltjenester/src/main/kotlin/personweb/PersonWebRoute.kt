package no.nav.etterlatte.personweb

import io.ktor.server.application.call
import io.ktor.server.request.path
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.ktor.brukerTokenInfo
import no.nav.etterlatte.libs.ktor.route.kunSaksbehandler

fun Route.personWebRoute(
    service: PersonWebService,
    sporing: SporingService,
) {
    route("/person") {
        post("/navn") {
            kunSaksbehandler {
                val request = call.receive<HentPersonNavnRequest>()

                val person = service.hentPersonNavn(request.ident, brukerTokenInfo)

                sporing.logg(brukerTokenInfo, person.foedselsnummer, call.request.path(), "Hentet navn på person")

                call.respond(person)
            }
        }
    }
}

private data class HentPersonNavnRequest(
    val ident: String,
)
