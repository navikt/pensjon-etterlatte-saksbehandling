package no.nav.etterlatte.enhetsregister

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.enhetsregApi(service: EnhetsregService) {
    route("enheter") {
        get {
            val navn = vaskEnhetsnavn(call.request.queryParameters["navn"]) {
                "Query param \"navn\" må være satt for å hente ut bedrifter."
            }

            val enheter = service.hentEnheter(navn)

            call.respond(enheter)
        }

        get("{orgnr}") {
            val orgnr = vaskEnhetsnavn(call.parameters["orgnr"]) {
                "Orgnr. må være satt for å hente ut enhet fra BRREG."
            }

            val enhet = service.hentEnhet(orgnr)

            call.respond(enhet ?: HttpStatusCode.NotFound)
        }

        get("statsforvalter") {
            call.respond(service.hentStatsforvalterListe())
        }
    }
}

internal fun vaskEnhetsnavn(input: String?, orElse: () -> String) = requireNotNull(input) {
    orElse
}.replaceBefore("://", "")
    .replace("://", "")

internal fun vaskOrgnr(input: String?, orElse: () -> String) = requireNotNull(input) {
    orElse
}.replace("[^0-9]".toRegex(), "")