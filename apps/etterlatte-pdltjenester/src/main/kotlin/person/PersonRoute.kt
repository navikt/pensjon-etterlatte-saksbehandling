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
import no.nav.etterlatte.libs.common.pdl.Variables
import no.nav.etterlatte.libs.common.person.EyFamilieRelasjon
import no.nav.etterlatte.libs.common.pdl.EyHentAdresseRequest
import no.nav.etterlatte.libs.common.pdl.EyHentFamilieRelasjonRequest
import no.nav.etterlatte.libs.common.pdl.EyHentUtvidetPersonRequest
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(PersonService::class.java)

/**
 * Endepunkter for uthenting av person
 */
fun Route.personApi(service: PersonService) {
    route("person") {

        post("hentperson") {
            val fnr = Foedselsnummer.of(call.receive<String>().toString())
            logger.info("Fnr: $fnr")
            val person = service.hentPerson(fnr)
            call.respond(person)
        }
        post("hentUtvidetperson") {
            val variables = EyHentUtvidetPersonRequest(call.receive<String>())

            logger.info("Fnr: " + variables.foedselsnummer)
            val person = service.hentPerson(Foedselsnummer.of(variables.foedselsnummer))
            if (variables.utland) {
                person.utland = service.hentUtland(person.foedselsnummer)
            }
            if (variables.adresse) {
                person.adresse = service.hentAdresse(EyHentAdresseRequest(person.foedselsnummer, variables.historikk))
            }
            if (variables.familieRelasjon) {
                person.familieRelasjon =
                    service.hentFamilieRelasjon(EyHentFamilieRelasjonRequest(person.foedselsnummer))
            }
            call.respond(person)
        }
        post("hentutland") {
            val fnr = Foedselsnummer.of(call.receive<String>().toString())
            logger.info("Fnr: $fnr")
            val utland = service.hentUtland(fnr)
            call.respond(utland)
        }
        post("hentadresse") {
            //val fnr = Foedselsnummer.of(call.receive<String>().toString())
            val variables = Variables(call.receive<String>().toString())
            //TODO litt usikker på om dette er beste måten å gjøre dette på
            //val historikk = (call.request.header("historikk")).toBoolean()
            //logger.info("Fnr: $fnr")
            val person =
                service.hentAdresse(EyHentAdresseRequest(Foedselsnummer.of(variables.ident), variables.historikk))

            call.respond(person)
        }
        post("hentfamilierelasjon") {
            val variables = Variables(call.receive<String>().toString())
            val familieRelasjon = service.hentFamilieRelasjon(
                EyHentFamilieRelasjonRequest(
                    Foedselsnummer.of(variables.ident),
                    variables.historikk
                )
            )
            call.respond(familieRelasjon)
        }
    }
}
