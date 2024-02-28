package no.nav.etterlatte.person

import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.kunSaksbehandler
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator

fun Route.personWebRoute(service: PersonService) {
    route("/person") {
        post("/navn") {
            kunSaksbehandler {
                val request = call.receive<HentPersonNavnRequest>()

                val person = service.hentPersonNavn(request.foedselsnummer)

                call.respond(person)
            }
        }
    }
}

private data class HentPersonNavnRequest(
    val foedselsnummer: Folkeregisteridentifikator,
)
