package no.nav.etterlatte.arbeidOgInntekt

import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.arbeidOgInntekt(arbeidOgInntektKlient: ArbeidOgInntektKlient) {
    route("/api/arbeid-og-inntekt") {
        post("/url-for-inntekt-oversikt") {
            val request = call.receive<UrlForInntektOversiktRequest>()

            call.respond(UrlForInntektOversiktResponse(arbeidOgInntektKlient.hentURLForInntektOversikt(fnr = request.fnr)))
        }
    }
}

data class UrlForInntektOversiktRequest(
    val fnr: String,
)

data class UrlForInntektOversiktResponse(
    val url: String?,
)
