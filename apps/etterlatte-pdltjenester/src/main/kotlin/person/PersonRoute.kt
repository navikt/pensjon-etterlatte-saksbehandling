package no.nav.etterlatte.person

import io.ktor.application.call
import io.ktor.features.BadRequestException
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import no.nav.etterlatte.libs.common.pdl.EyHentUtvidetPersonRequest
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(PersonService::class.java)


/**
 * Endepunkter for uthenting av person
 */

fun Route.personApi(service: PersonService) {
    route("person") {

        //TODO Depricated: slette etterhvert
        post("hentperson") {
            val fnr = Foedselsnummer.of(call.receive<String>().toString())
            logger.info("Henter person med fnr=$fnr")
            service.hentPerson(EyHentUtvidetPersonRequest(fnr.value))
                .let { call.respond(it) }
        }

        get {
            val queryParams = call.request.queryParameters
            val headers = call.request.headers

            val variables = EyHentUtvidetPersonRequest(
                foedselsnummer = headers["foedselsnummer"]
                    ?: throw BadRequestException("foedselsnummer is a required header"),
                historikk = queryParams["historikk"]?.toBoolean() ?: false,
                adresse = queryParams["adresse"]?.toBoolean() ?: false,
                utland = queryParams["utland"]?.toBoolean() ?: false,
                familieRelasjon = queryParams["familieRelasjon"]?.toBoolean() ?: false
            )

            logger.info("Henter person med fnr=${Foedselsnummer.of(variables.foedselsnummer)}")
            service.hentPerson(variables)
                .let { call.respond(it) }
        }
    }
}

