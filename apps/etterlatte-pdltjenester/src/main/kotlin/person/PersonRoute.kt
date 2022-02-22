package no.nav.etterlatte.person

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import no.nav.etterlatte.libs.common.person.HentPersonRequest
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(PersonService::class.java)


/**
 * Endepunkter for uthenting av person
 */

fun Route.personApi(service: PersonService) {
    route("person") {

        //TODO skrive om så den kan ta imot EyHentUtvidetPErsonRequest input
        post("hentperson") {
            val fnr = Foedselsnummer.of(call.receive<String>().toString())
            logger.info("Henter person med fnr=$fnr")
            service.hentPerson(HentPersonRequest(fnr))
                .let { call.respond(it) }
        }

        get {
            val queryParams = call.request.queryParameters
            val headers = call.request.headers

            val variables = HentPersonRequest(
                foedselsnummer = Foedselsnummer.of(headers["foedselsnummer"]),
                historikk = queryParams["historikk"]?.toBoolean() ?: false,
                adresse = queryParams["adresse"]?.toBoolean() ?: false,
                utland = queryParams["utland"]?.toBoolean() ?: false,
                familieRelasjon = queryParams["familieRelasjon"]?.toBoolean() ?: false
            )

            logger.info("Henter person med fnr=${variables.foedselsnummer}")
            service.hentPerson(variables)
                .let { call.respond(it) }
        }

        post {
            try {
                val hentPersonRequest = call.receive<HentPersonRequest>()
                logger.info("Henter person med fnr=${hentPersonRequest.foedselsnummer}")

                service.hentPerson(hentPersonRequest)
                    .let { call.respond(it) }

            } catch (t: Throwable) {
                logger.error("En feil oppstod ved uthenting av person fra PDL: ${t.message}", t)
                call.respond(
                    status = HttpStatusCode.InternalServerError,
                    message = "En feil oppstod ved uthenting av person mot PDL"
                )
            }
        }

    }
}

