package no.nav.etterlatte.libs.ktor

import io.ktor.http.auth.HttpAuthHeader
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.parseAuthorizationHeader
import io.ktor.server.auth.principal
import io.ktor.util.pipeline.PipelineContext
import no.nav.security.token.support.v2.TokenValidationContextPrincipal

inline val PipelineContext<*, ApplicationCall>.saksbehandler: String
    get() = requireNotNull(
        call.principal<TokenValidationContextPrincipal>()
            ?.context?.getJwtToken("azure")
            ?.jwtTokenClaims?.getStringClaim("NAVident")
    )

inline val PipelineContext<*, ApplicationCall>.accesstoken: String
    get() = call.request.parseAuthorizationHeader().let {
        if (!(it == null || it !is HttpAuthHeader.Single || it.authScheme != "Bearer")) {
            it.blob
        } else {
            throw Exception("Missing authorization header")
        }
    }