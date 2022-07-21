package no.nav.etterlatte.enhetsregister

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.enhetsregApi(service: EnhetsregService) {
    route("enheter") {
        get {
            val navn = requireNotNull(call.request.queryParameters["navn"]) {
                "Query param \"navn\" må være satt for å hente ut bedrifter."
            }

            val enheter = service.hentEnheter(navn)

            call.respond(enheter)
        }

        get("{orgnr}") {
            val orgnr = requireNotNull(call.parameters["orgnr"]) {
                "Orgnr. må være satt for å hente ut enhet fra BRREG."
            }

            val enhet = service.hentEnhet(orgnr)

            call.respond(enhet ?: HttpStatusCode.NotFound)
        }
    }
}
