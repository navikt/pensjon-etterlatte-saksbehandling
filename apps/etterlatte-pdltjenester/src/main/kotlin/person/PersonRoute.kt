package no.nav.etterlatte.person

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import io.ktor.routing.route
import no.nav.etterlatte.libs.common.person.HentPersonRequest
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(PersonService::class.java)


fun Route.personApi(service: PersonService) {
    route("person") {
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

