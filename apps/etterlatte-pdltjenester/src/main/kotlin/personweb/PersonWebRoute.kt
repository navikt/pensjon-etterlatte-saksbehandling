package no.nav.etterlatte.personweb

import io.ktor.server.application.call
import io.ktor.server.request.path
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.ktor.brukerTokenInfo
import no.nav.etterlatte.libs.ktor.route.kunSaksbehandler
import no.nav.etterlatte.pdl.SoekPerson

fun Route.personWebRoute(
    service: PersonWebService,
    sporing: SporingService,
) {
    route("/person") {
        post("/navn-foedsel") {
            kunSaksbehandler {
                val request = call.receive<HentPersonDetaljerIdentRequest>()

                val person = service.hentPersonNavnOgFoedsel(request.ident, brukerTokenInfo)

                sporing.logg(brukerTokenInfo, person.foedselsnummer, call.request.path(), "Hentet navn på person")

                call.respond(person)
            }
        }

        post("/soek") {
            kunSaksbehandler {
                val request = call.receive<SoekPerson>()
                val personerFunnet = service.soekPerson(request, brukerTokenInfo)
                call.respond(personerFunnet)
                // sporing.logg(brukerTokenInfo, person.foedselsnummer, call.request.path(), "Hentet navn på person")
            }
        }

        post("/familieOpplysninger") {
            kunSaksbehandler {
                val request = call.receive<HentFamilieOpplysningerRequest>()

                val personopplysninger = service.hentFamilieOpplysninger(request.ident, request.sakType, brukerTokenInfo)

                sporing.logg(
                    brukerTokenInfo,
                    Folkeregisteridentifikator.of(request.ident),
                    call.request.path(),
                    "Hentet familie opplysninger på person",
                )

                call.respond(personopplysninger)
            }
        }
    }
}

private data class HentPersonDetaljerIdentRequest(
    val ident: String,
)

private data class HentFamilieOpplysningerRequest(
    val ident: String,
    val sakType: SakType,
)
