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

fun Route.personWebRoute(
    service: PersonWebService,
    sporing: SporingService,
) {
    route("/person") {
        post("/navn") {
            kunSaksbehandler {
                val request = call.receive<HentPersonNavnRequest>()

                val person = service.hentPersonNavn(request.ident, brukerTokenInfo)

                sporing.logg(brukerTokenInfo, person.foedselsnummer, call.request.path(), "Hentet navn på person")

                call.respond(person)
            }
        }

        post("/opplysninger") {
            kunSaksbehandler {
                val request = call.receive<HentPersongalleriRequest>()

                val personopplysninger = service.hentPersonopplysninger(request.ident, request.sakType, brukerTokenInfo)

                sporing.logg(
                    brukerTokenInfo,
                    Folkeregisteridentifikator.of(request.ident),
                    call.request.path(),
                    "Hentet persongalleri på person",
                )

                call.respond(personopplysninger)
            }
        }
    }
}

private data class HentPersonNavnRequest(
    val ident: String,
)

private data class HentPersongalleriRequest(
    val ident: String,
    val sakType: SakType,
)
