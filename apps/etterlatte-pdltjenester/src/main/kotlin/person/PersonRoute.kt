package no.nav.etterlatte.person

import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeFunnetException
import no.nav.etterlatte.libs.common.person.HentAdressebeskyttelseRequest
import no.nav.etterlatte.libs.common.person.HentGeografiskTilknytningRequest
import no.nav.etterlatte.libs.common.person.HentPdlIdentRequest
import no.nav.etterlatte.libs.common.person.HentPersonHistorikkForeldreAnsvarRequest
import no.nav.etterlatte.libs.common.person.HentPersonRequest
import no.nav.etterlatte.libs.common.person.HentPersongalleriRequest
import no.nav.etterlatte.libs.ktor.route.kunSaksbehandler
import no.nav.etterlatte.libs.ktor.route.medBody
import no.nav.etterlatte.sikkerLogg
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

            post("doedshendelse") {
                val hentPersonRequest = call.receive<HentPersonRequest>()
                logger.info("Henter personpplysning med fnr=${hentPersonRequest.foedselsnummer}")
                call.respond(service.hentDoedshendelseOpplysningsperson(hentPersonRequest))
            }
        }

        post("/adressebeskyttelse") {
            val request = call.receive<HentAdressebeskyttelseRequest>()
            logger.info("Henter adressebeskyttelse/gradering for fnr=${request.ident}")

            call.respond(service.hentAdressebeskyttelseGradering(request))
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

    post("folkeregisteridenter") {
        val request = call.receive<HentPdlIdentRequest>()

        val identer = service.hentPdlFolkeregisterIdenter(request)

        call.respond(identer)
    }

    post("foedselsdato") {
        kunSaksbehandler {
            val request = call.receive<HentPdlIdentRequest>()

            val foedselsdato =
                service.hentFoedselsdato(request.ident.value).foedselsdato
                    ?: throw IkkeFunnetException("IKKE_FUNNET", "Fant ingen fødselsdato for bruker (se sikkerlogg)")

            sikkerLogg.error("Fant ingen fødselsdato i PDL for ident=${request.ident.value}")

            call.respond(foedselsdato)
        }
    }

    route("aktoerid") {
        post {
            val ident = call.receive<HentPdlIdentRequest>()

            val aktoerId = service.hentAktoerId(ident)

            call.respond(aktoerId)
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
            val identRequest = call.receive<HentPersonHistorikkForeldreAnsvarRequest>()
            logger.info("Henter historikk for foreldreansvar for person med fnr=${identRequest.foedselsnummer}")
            service.hentHistorikkForeldreansvar(identRequest).let { call.respond(it) }
        }
    }
}
