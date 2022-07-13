package no.nav.etterlatte.person

import io.ktor.application.call
import io.ktor.application.log
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.application
import io.ktor.routing.post
import io.ktor.routing.route
import no.nav.etterlatte.libs.common.person.HentFolkeregisterIdentRequest
import no.nav.etterlatte.libs.common.person.HentPersonRequest


fun Route.personApi(service: PersonService) {
    route("person") {
        val logger = application.log

        post {
            val hentPersonRequest = call.receive<HentPersonRequest>()
            logger.info("Henter person med fnr=${hentPersonRequest.foedselsnummer}")

            service.hentPerson(hentPersonRequest)
                .let { call.respond(it) }
        }
    }

    route("folkeregisterident") {
        val logger = application.log

        post {
            val hentFolkeregisterIdentRequest = call.receive<HentFolkeregisterIdentRequest>()
            logger.info("Henter identer for ident=${hentFolkeregisterIdentRequest.ident}")

            service.hentFolkeregisterIdent(hentFolkeregisterIdentRequest).let {
                call.respond(it)
            }
        }
    }
}
