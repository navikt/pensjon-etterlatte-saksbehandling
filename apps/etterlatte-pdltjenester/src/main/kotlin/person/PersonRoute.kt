package no.nav.etterlatte.person

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.person.HentFolkeregisterIdentRequest
import no.nav.etterlatte.libs.common.person.HentGeografiskTilknytningRequest
import no.nav.etterlatte.libs.common.person.HentPersonRequest

fun Route.personRoute(service: PersonService) {
    route("person") {
        val logger = application.log

        post {
            val hentPersonRequest = call.receive<HentPersonRequest>()
            logger.info("Henter person med fnr=${hentPersonRequest.folkeregisteridentifikator}")

            try {
                service.hentPerson(hentPersonRequest).let { call.respond(it) }
            } catch (e: PdlFantIkkePerson) {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        route("/v2") {
            post {
                val hentPersonRequest = call.receive<HentPersonRequest>()
                logger.info("Henter personopplysning med fnr=${hentPersonRequest.folkeregisteridentifikator}")

                try {
                    service.hentOpplysningsperson(hentPersonRequest).let { call.respond(it) }
                } catch (e: PdlFantIkkePerson) {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
    }

    route("folkeregisterident") {
        val logger = application.log

        post {
            val hentFolkeregisterIdentRequest = call.receive<HentFolkeregisterIdentRequest>()
            logger.info("Henter identer for ident=${hentFolkeregisterIdentRequest.ident}")

            try {
                service.hentFolkeregisterIdent(hentFolkeregisterIdentRequest).let { call.respond(it) }
            } catch (e: PdlFantIkkePerson) {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }

    route("geografisktilknyttning") {
        val logger = application.log

        post {
            val hentGeografiskTilknytningRequest = call.receive<HentGeografiskTilknytningRequest>()
            logger.info(
                "Henter geografisk tilknyttning med fnr=${hentGeografiskTilknytningRequest.folkeregisteridentifikator}"
            )

            try {
                service.hentGeografiskTilknytning(hentGeografiskTilknytningRequest).let { call.respond(it) }
            } catch (e: PdlFantIkkePerson) {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
}