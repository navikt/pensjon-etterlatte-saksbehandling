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
import no.nav.etterlatte.libs.ktor.route.kunSystembruker
import no.nav.etterlatte.libs.ktor.route.medBody
import no.nav.etterlatte.sikkerLogg
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("PersonRoute")

fun Route.personRoute(service: PersonService) {
    // Rutene under er kun for maskin-til-maskin-kall og har ingen egen tilgangssjekk eller sporingslogg.
    // Oppslag fra saksbehandlere skal gå via personWebRoute.
    route("person") {
        post {
            kunSystembruker {
                val hentPersonRequest = call.receive<HentPersonRequest>()
                logger.info("Henter person med fnr=${hentPersonRequest.foedselsnummer}")

                call.respond(service.hentPerson(hentPersonRequest))
            }
        }

        route("/v2") {
            post {
                kunSystembruker {
                    val hentPersonRequest = call.receive<HentPersonRequest>()
                    logger.info("Henter personopplysning med fnr=${hentPersonRequest.foedselsnummer}")

                    call.respond(service.hentOpplysningsperson(hentPersonRequest))
                }
            }

            post("doedshendelse") {
                kunSystembruker {
                    val hentPersonRequest = call.receive<HentPersonRequest>()
                    logger.info("Henter personpplysning med fnr=${hentPersonRequest.foedselsnummer}")
                    call.respond(service.hentDoedshendelseOpplysningsperson(hentPersonRequest))
                }
            }
        }

        post("/adressebeskyttelse") {
            kunSystembruker {
                val request = call.receive<HentAdressebeskyttelseRequest>()
                logger.info("Henter adressebeskyttelse/gradering for fnr=${request.ident}")

                call.respond(service.hentAdressebeskyttelseGradering(request))
            }
        }
    }

    route("galleri") {
        post {
            kunSystembruker {
                medBody<HentPersongalleriRequest> { hentPersongalleriRequest ->
                    logger.info(
                        "Henter persongalleri for ${hentPersongalleriRequest.saktype}-saken " +
                            "til ${hentPersongalleriRequest.mottakerAvYtelsen}",
                    )

                    call.respond(service.hentPersongalleri(hentPersongalleriRequest))
                }
            }
        }
    }

    route("pdlident") {
        post {
            kunSystembruker {
                val hentPdlIdentRequest = call.receive<HentPdlIdentRequest>()
                logger.info("Henter identer for ident=${hentPdlIdentRequest.ident}")

                val pdlIdentifikator = service.hentPdlIdentifikator(hentPdlIdentRequest)

                call.respond(pdlIdentifikator)
            }
        }
    }

    post("folkeregisteridenter") {
        kunSystembruker {
            val request = call.receive<HentPdlIdentRequest>()

            val identer = service.hentPdlFolkeregisterIdenter(request)

            call.respond(identer)
        }
    }

    post("foedselsdato") {
        kunSaksbehandler {
            val request = call.receive<HentPdlIdentRequest>()

            val foedselsdato = service.hentFoedselsdato(request.ident.value).foedselsdato

            if (foedselsdato == null) {
                sikkerLogg.error("Fant ingen fødselsdato i PDL for ident=${request.ident.value}")
                throw IkkeFunnetException("IKKE_FUNNET", "Fant ingen fødselsdato for bruker (se sikkerlogg)")
            }

            call.respond(foedselsdato)
        }
    }

    route("aktoerid") {
        post {
            kunSystembruker {
                val ident = call.receive<HentPdlIdentRequest>()

                call.respond(service.hentAktoerId(ident))
            }
        }
    }

    route("geografisktilknytning") {
        post {
            kunSystembruker {
                val hentGeografiskTilknytningRequest = call.receive<HentGeografiskTilknytningRequest>()
                logger.info("Henter geografisk tilknytning med fnr=${hentGeografiskTilknytningRequest.foedselsnummer}")

                call.respond(service.hentGeografiskTilknytning(hentGeografiskTilknytningRequest))
            }
        }
    }

    route("foreldreansvar") {
        post {
            kunSystembruker {
                val identRequest = call.receive<HentPersonHistorikkForeldreAnsvarRequest>()
                logger.info("Henter historikk for foreldreansvar for person med fnr=${identRequest.foedselsnummer}")

                call.respond(service.hentHistorikkForeldreansvar(identRequest))
            }
        }
    }
}
