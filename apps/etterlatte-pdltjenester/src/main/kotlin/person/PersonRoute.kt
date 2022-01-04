package no.nav.etterlatte.person

import io.ktor.application.call
import io.ktor.request.receive
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import no.nav.etterlatte.common.innloggetBrukerFnr
import no.nav.etterlatte.common.toJson
import no.nav.etterlatte.libs.common.person.Foedselsnummer

/**
 * Endepunkter for uthenting av person
 */
fun Route.personApi(service: PersonService) {
    route("person") {
        get("innlogget") {
            val fnr = innloggetBrukerFnr()

            val person = service.hentPerson(fnr)

            call.respondText(person.toJson())
        }

        post("hentperson") {
            val fnr = Foedselsnummer.of(call.receive<String>().toString())

            val person = service.hentPerson(fnr)

            call.respondText(person.toJson())
        }
    }
}
