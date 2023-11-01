package no.nav.etterlatte.person

import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.medBody
import no.nav.etterlatte.libs.common.person.HentFolkeregisterIdenterForAktoerIdBolkRequest
import no.nav.etterlatte.libs.common.person.HentGeografiskTilknytningRequest
import no.nav.etterlatte.libs.common.person.HentPdlIdentRequest
import no.nav.etterlatte.libs.common.person.HentPersonRequest
import no.nav.etterlatte.libs.common.person.HentPersongalleriRequest
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("PersonRoute")

fun Route.personRoute(service: PersonService) {
    route("person") {
        post {
            val hentPersonRequest = call.receive<HentPersonRequest>()
            logger.info("Henter person med fnr=${hentPersonRequest.foedselsnummer}")

            service.hentPerson(hentPersonRequest).let { call.respond(it) }
        }

        route("/v2") {
            post {
                val hentPersonRequest = call.receive<HentPersonRequest>()
                logger.info("Henter personopplysning med fnr=${hentPersonRequest.foedselsnummer}")

                service.hentOpplysningsperson(hentPersonRequest).let { call.respond(it) }
            }
        }
    }

    route("galleri") {
        post {
            medBody<HentPersongalleriRequest> { hentPersongalleriRequest ->
                logger.info(
                    "Henter persongalleri for ${hentPersongalleriRequest.saktype}-saken " +
                        "til ${hentPersongalleriRequest.mottakerAvYtelsen}",
                )

                val persongalleri = service.hentPersongalleri(hentPersongalleriRequest)
                call.respond(persongalleri)
            }
        }
    }

    route("pdlident") {
        post {
            val hentPdlIdentRequest = call.receive<HentPdlIdentRequest>()
            logger.info("Henter identer for ident=${hentPdlIdentRequest.ident}")

            service.hentPdlIdentifikator(hentPdlIdentRequest).let { call.respond(it) }
        }
    }

    route("folkeregisteridenter") {
        post {
            val personIdenterForAktoerIdRequest = call.receive<HentFolkeregisterIdenterForAktoerIdBolkRequest>()

            service.hentFolkeregisterIdenterForAktoerIdBolk(
                personIdenterForAktoerIdRequest,
            ).let { call.respond(it) }
        }
    }

    route("geografisktilknytning") {
        post {
            val hentGeografiskTilknytningRequest = call.receive<HentGeografiskTilknytningRequest>()
            logger.info("Henter geografisk tilknytning med fnr=${hentGeografiskTilknytningRequest.foedselsnummer}")

            service.hentGeografiskTilknytning(hentGeografiskTilknytningRequest).let { call.respond(it) }
        }
    }

    route("foreldreansvar") {
        post {
            val identRequest = call.receive<HentPersonRequest>()
            logger.info("Henter historikk for foreldreansvar for person med fnr=${identRequest.foedselsnummer}")
            service.hentHistorikkForeldreansvar(identRequest).let { call.respond(it) }
        }
    }
}
