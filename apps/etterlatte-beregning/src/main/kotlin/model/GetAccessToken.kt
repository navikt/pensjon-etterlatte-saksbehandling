package no.nav.etterlatte.model

import io.ktor.http.auth.HttpAuthHeader
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.parseAuthorizationHeader

fun getAccessToken(call: ApplicationCall): String {
    val authHeader = call.request.parseAuthorizationHeader()
    if (!(
        authHeader == null ||
            authHeader !is HttpAuthHeader.Single ||
            authHeader.authScheme != "Bearer"
        )
    ) {
        return authHeader.blob
    }
    throw Exception("Missing authorization header")
}