package no.nav.etterlatte.person

import io.ktor.application.call
import io.ktor.features.BadRequestException
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

        //TODO Depricated: slette etterhvert
        post("hentperson") {
            val fnr = Foedselsnummer.of(call.receive<String>().toString())
            logger.info("Fnr: $fnr")
            val person = service.hentPerson(EyHentUtvidetPersonRequest(fnr.value))
            call.respond(person)
        }
        get("") {
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

            logger.info("Henter person med fnr=${variables.foedselsnummer}")
            service.hentPerson(variables)
                .let { call.respond(it) }
        }
        //TODO Depricated: slette etterhvert
        get("utvidetperson") {
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

            logger.info("Henter person med fnr=${variables.foedselsnummer}")
            service.hentPerson(variables)
                .let { call.respond(it) }
        }
    }
}
