package no.nav.etterlatte.person

import io.ktor.application.call
import io.ktor.request.*
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import no.nav.etterlatte.common.innloggetBrukerFnr
import no.nav.etterlatte.common.toJson
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(PersonService::class.java)

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
            logger.info("Fnr: $fnr")
            val person = service.hentPerson(fnr)
            call.respond(person)
        }
        post("hentutland") {
            val fnr = Foedselsnummer.of(call.receive<String>().toString())
            logger.info("Fnr: $fnr")
            val utland = service.hentUtland(fnr)
            call.respond(utland)
        }
        post("hentadresse") {
            val fnr = Foedselsnummer.of(call.receive<String>().toString())
            //TODO litt usikker på om dette er beste måten å gjøre dette på
            val historikk = (call.request.header("historikk")).toBoolean()
            logger.info("Fnr: $fnr")
            val person = service.hentAdresse(fnr, historikk)
            call.respond(person)
        }
    }
}
