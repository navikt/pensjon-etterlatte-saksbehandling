package no.nav.etterlatte

import io.ktor.http.auth.HttpAuthHeader
import io.ktor.server.application.*
import io.ktor.server.auth.*

fun getAccessToken(call: ApplicationCall): String {
    val authHeader = call.request.parseAuthorizationHeader()
    if (!(authHeader == null
                || authHeader !is HttpAuthHeader.Single
                || authHeader.authScheme != "Bearer")
    ) {
        return authHeader.blob
    }
    throw Exception("Missing authorization header")
}