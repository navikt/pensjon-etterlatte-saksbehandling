package no.nav.etterlatte.person

import io.ktor.application.ApplicationCall
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.features.BadRequestException
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import no.nav.etterlatte.libs.common.logging.CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.withLogContextCo
import no.nav.etterlatte.libs.common.pdl.EyHentUtvidetPersonRequest
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(PersonService::class.java)


/**
 * Endepunkter for uthenting av person
 */

fun Route.personApi(service: PersonService) {
    route("person") {

        intercept(ApplicationCallPipeline.Setup) {
            logger.info("CorrelationId: ${call.correlationId()}")
            withLogContextCo(call.correlationId()) {
                proceed()
            }
        }

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

fun ApplicationCall.correlationId(): String? = this.request.headers[CORRELATION_ID]

