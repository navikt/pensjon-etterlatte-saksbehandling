package no.nav.etterlatte.medl

import io.ktor.http.HttpStatusCode
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.parseAuthorizationHeader
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.medlemsregisterApi(klient: MedlRegisterKlient) {
    route("medl") {
        get("medlemsunntak") {
            val ident = call.parameters["ident"]!!
            val tokenxToken = getAccessToken(call)

            val medlemskapsunntak = klient.hentMedlemskapsunntak(ident, tokenxToken)

            call.respond(medlemskapsunntak ?: HttpStatusCode.NotFound)
        }

        get("innsyn/person/{ident}") {
            val ident = call.parameters["ident"]!!
            val tokenxToken = getAccessToken(call)

            val innsyn = klient.hentInnsyn(ident, tokenxToken)

            call.respond(innsyn ?: HttpStatusCode.NotFound)
        }

    }
}

private fun getAccessToken(call: ApplicationCall): String {
    val header = call.request.parseAuthorizationHeader()

    if (header is HttpAuthHeader.Single && header.authScheme == "Bearer") {
        return header.blob
    }

    throw Exception("Missing authorization header")
}
