package no.nav.etterlatte.person

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
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
