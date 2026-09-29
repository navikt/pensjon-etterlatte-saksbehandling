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

            val person =
                personOppslag(
                    operasjon = "Henting av person",
                    folkeregisteridentifikator = hentPersonRequest.foedselsnummer.value,
                ) {
                    service.hentPerson(hentPersonRequest)
                }
            call.respond(person)
        }

        route("/v2") {
            post {
                val hentPersonRequest = call.receive<HentPersonRequest>()
                logger.info("Henter personopplysning med fnr=${hentPersonRequest.foedselsnummer}")

                val person =
                    personOppslag(
                        operasjon = "Henting av personopplysninger",
                        folkeregisteridentifikator = hentPersonRequest.foedselsnummer.value,
                    ) {
                        service.hentOpplysningsperson(hentPersonRequest)
                    }
                call.respond(person)
            }

            post("doedshendelse") {
                val hentPersonRequest = call.receive<HentPersonRequest>()
                logger.info("Henter personpplysning med fnr=${hentPersonRequest.foedselsnummer}")
                val person =
                    personOppslag(
                        operasjon = "Henting av personopplysninger for dødshendelse",
                        folkeregisteridentifikator = hentPersonRequest.foedselsnummer.value,
                    ) {
                        service.hentDoedshendelseOpplysningsperson(hentPersonRequest)
                    }
                call.respond(person)
            }
        }

        post("/adressebeskyttelse") {
            val request = call.receive<HentAdressebeskyttelseRequest>()
            logger.info("Henter adressebeskyttelse/gradering for fnr=${request.ident}")

            val adressebeskyttelse =
                personOppslag(
                    operasjon = "Henting av adressebeskyttelse",
                    folkeregisteridentifikator = request.ident.value,
                ) {
                    service.hentAdressebeskyttelseGradering(request)
                }
            call.respond(adressebeskyttelse)
        }
    }

    route("galleri") {
        post {
            medBody<HentPersongalleriRequest> { hentPersongalleriRequest ->
                logger.info(
                    "Henter persongalleri for ${hentPersongalleriRequest.saktype}-saken " +
                        "til ${hentPersongalleriRequest.mottakerAvYtelsen}",
                )

                val persongalleri =
                    personOppslag(
                        operasjon = "Henting av persongalleri",
                        folkeregisteridentifikatorer =
                            listOfNotNull(
                                hentPersongalleriRequest.mottakerAvYtelsen.value,
                                hentPersongalleriRequest.innsender?.value,
                            ),
                    ) {
                        service.hentPersongalleri(hentPersongalleriRequest)
                    }
                call.respond(persongalleri)
            }
        }
    }

    route("pdlident") {
        post {
            val hentPdlIdentRequest = call.receive<HentPdlIdentRequest>()
            logger.info("Henter identer for ident=${hentPdlIdentRequest.ident}")

            val pdlIdentifikator =
                personOppslag(
                    operasjon = "Henting av PDL-identifikator",
                    folkeregisteridentifikator = hentPdlIdentRequest.ident.value,
                ) {
                    service.hentPdlIdentifikator(hentPdlIdentRequest)
                }
            call.respond(pdlIdentifikator)
        }
    }

    post("folkeregisteridenter") {
        val request = call.receive<HentPdlIdentRequest>()

        val identer =
            personOppslag(
                operasjon = "Henting av folkeregisteridenter",
                folkeregisteridentifikator = request.ident.value,
            ) {
                service.hentPdlFolkeregisterIdenter(request)
            }

        call.respond(identer)
    }

    post("foedselsdato") {
        kunSaksbehandler {
            val request = call.receive<HentPdlIdentRequest>()

            val foedselsdato =
                personOppslag(
                    operasjon = "Henting av fødselsdato",
                    folkeregisteridentifikator = request.ident.value,
                ) {
                    service.hentFoedselsdato(request.ident.value)
                }.foedselsdato

            if (foedselsdato == null) {
                sikkerLogg.error("Fant ingen fødselsdato i PDL for ident=${request.ident.value}")
                throw IkkeFunnetException("IKKE_FUNNET", "Fant ingen fødselsdato for bruker (se sikkerlogg)")
            }

            call.respond(foedselsdato)
        }
    }

    route("aktoerid") {
        post {
            val ident = call.receive<HentPdlIdentRequest>()

            val aktoerId =
                personOppslag(
                    operasjon = "Henting av aktør-ID",
                    folkeregisteridentifikator = ident.ident.value,
                ) {
                    service.hentAktoerId(ident)
                }

            call.respond(aktoerId)
        }
    }

    route("geografisktilknytning") {
        post {
            val hentGeografiskTilknytningRequest = call.receive<HentGeografiskTilknytningRequest>()
            logger.info("Henter geografisk tilknytning med fnr=${hentGeografiskTilknytningRequest.foedselsnummer}")

            val geografiskTilknytning =
                personOppslag(
                    operasjon = "Henting av geografisk tilknytning",
                    folkeregisteridentifikator = hentGeografiskTilknytningRequest.foedselsnummer.value,
                ) {
                    service.hentGeografiskTilknytning(hentGeografiskTilknytningRequest)
                }
            call.respond(geografiskTilknytning)
        }
    }

    route("foreldreansvar") {
        post {
            val identRequest = call.receive<HentPersonHistorikkForeldreAnsvarRequest>()
            logger.info("Henter historikk for foreldreansvar for person med fnr=${identRequest.foedselsnummer}")
            val foreldreansvar =
                personOppslag(
                    operasjon = "Henting av historikk for foreldreansvar",
                    folkeregisteridentifikator = identRequest.foedselsnummer.value,
                ) {
                    service.hentHistorikkForeldreansvar(identRequest)
                }
            call.respond(foreldreansvar)
        }
    }
}
