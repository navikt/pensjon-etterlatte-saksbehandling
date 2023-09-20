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
import no.nav.etterlatte.libs.common.person.HentFolkeregisterIdenterForAktoerIdBolkRequest
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
                call.respond(HttpStatusCode.NotFound, e.tilPdlFeil())
            } catch (e: FamilieRelasjonManglerIdent) {
                call.respond(HttpStatusCode.InternalServerError, e.tilPdlFeil())
            }
        }

        route("/v2") {
            post {
                val hentPersonRequest = call.receive<HentPersonRequest>()
                logger.info("Henter personopplysning med fnr=${hentPersonRequest.foedselsnummer}")

                try {
                    service.hentOpplysningsperson(hentPersonRequest).let { call.respond(it) }
                } catch (e: PdlFantIkkePerson) {
                    call.respond(HttpStatusCode.NotFound, e.tilPdlFeil())
                } catch (e: FamilieRelasjonManglerIdent) {
                    call.respond(HttpStatusCode.InternalServerError, e.tilPdlFeil())
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

    route("folkeregisteridenter") {
        post {
            val personIdenterForAktoerIdRequest = call.receive<HentFolkeregisterIdenterForAktoerIdBolkRequest>()

            try {
                service.hentFolkeregisterIdenterForAktoerIdBolk(
                    personIdenterForAktoerIdRequest,
                ).let { call.respond(it) }
            } catch (e: PdlFantIkkePerson) {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }

    route("geografisktilknytning") {
        val logger = application.log

        post {
            val hentGeografiskTilknytningRequest = call.receive<HentGeografiskTilknytningRequest>()
            logger.info("Henter geografisk tilknytning med fnr=${hentGeografiskTilknytningRequest.foedselsnummer}")

            try {
                service.hentGeografiskTilknytning(hentGeografiskTilknytningRequest).let { call.respond(it) }
            } catch (e: PdlFantIkkePerson) {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }

    route("foreldreansvar") {
        val logger = application.log

        post {
            val identRequest = call.receive<HentPersonRequest>()
            logger.info("Henter historikk for foreldreansvar for person med fnr=${identRequest.foedselsnummer}")
            try {
                service.hentHistorikkForeldreansvar(identRequest).let { call.respond(it) }
            } catch (e: PdlFantIkkePerson) {
                call.respond(HttpStatusCode.NotFound, e.tilPdlFeil())
            } catch (e: FamilieRelasjonManglerIdent) {
                call.respond(HttpStatusCode.InternalServerError, e.tilPdlFeil())
            }
        }
    }
}

fun PdlFantIkkePerson.tilPdlFeil(): PdlFeil = PdlFeil(aarsak = PdlFeilAarsak.FANT_IKKE_PERSON, detaljer = this.message)

fun FamilieRelasjonManglerIdent.tilPdlFeil(): PdlFeil = PdlFeil(aarsak = PdlFeilAarsak.INGEN_IDENT_FAMILIERELASJON, detaljer = this.message)
