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
import no.nav.etterlatte.libs.common.pdl.PdlFeil
import no.nav.etterlatte.libs.common.pdl.PdlFeilAarsak
import no.nav.etterlatte.libs.common.person.FamilieRelasjonManglerIdent
import no.nav.etterlatte.libs.common.person.HentGeografiskTilknytningRequest
import no.nav.etterlatte.libs.common.person.HentPdlIdentRequest
import no.nav.etterlatte.libs.common.person.HentPersonRequest

fun Route.personRoute(service: PersonService) {
    route("person") {
        val logger = application.log

        post {
            val hentPersonRequest = call.receive<HentPersonRequest>()
            logger.info("Henter person med fnr=${hentPersonRequest.foedselsnummer}")

            try {
                service.hentPerson(hentPersonRequest).let { call.respond(it) }
            } catch (e: PdlFantIkkePerson) {
                call.respond(
                    HttpStatusCode.NotFound,
                    PdlFeil(
                        aarsak = PdlFeilAarsak.FANT_IKKE_PERSON,
                        detaljer = "Fnr ${hentPersonRequest.foedselsnummer} finnes ikke i PDL"
                    )
                )
            } catch (e: FamilieRelasjonManglerIdent) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    PdlFeil(
                        aarsak = PdlFeilAarsak.INGEN_IDENT_FAMILIERELASJON,
                        detaljer = e.message
                    )
                )
            }
        }

        route("/v2") {
            post {
                val hentPersonRequest = call.receive<HentPersonRequest>()
                logger.info("Henter personopplysning med fnr=${hentPersonRequest.foedselsnummer}")

                try {
                    service.hentOpplysningsperson(hentPersonRequest).let { call.respond(it) }
                } catch (e: PdlFantIkkePerson) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        PdlFeil(
                            aarsak = PdlFeilAarsak.FANT_IKKE_PERSON,
                            detaljer = "Fnr ${hentPersonRequest.foedselsnummer} finnes ikke i PDL"
                        )
                    )
                } catch (e: FamilieRelasjonManglerIdent) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        PdlFeil(
                            aarsak = PdlFeilAarsak.INGEN_IDENT_FAMILIERELASJON,
                            detaljer = e.message
                        )
                    )
                }
            }
        }
    }

    route("pdlident") {
        val logger = application.log

        post {
            val hentPdlIdentRequest = call.receive<HentPdlIdentRequest>()
            logger.info("Henter identer for ident=${hentPdlIdentRequest.ident}")

            try {
                service.hentPdlIdentifikator(hentPdlIdentRequest).let { call.respond(it) }
            } catch (e: PdlFantIkkePerson) {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }

    route("geografisktilknyttning") {
        val logger = application.log

        post {
            val hentGeografiskTilknytningRequest = call.receive<HentGeografiskTilknytningRequest>()
            logger.info("Henter geografisk tilknyttning med fnr=${hentGeografiskTilknytningRequest.foedselsnummer}")

            try {
                service.hentGeografiskTilknytning(hentGeografiskTilknytningRequest).let { call.respond(it) }
            } catch (e: PdlFantIkkePerson) {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
}