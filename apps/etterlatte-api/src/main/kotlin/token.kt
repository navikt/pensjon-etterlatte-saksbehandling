package no.nav.etterlatte

import io.ktor.application.ApplicationCall
import io.ktor.auth.parseAuthorizationHeader
import io.ktor.http.auth.HttpAuthHeader

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