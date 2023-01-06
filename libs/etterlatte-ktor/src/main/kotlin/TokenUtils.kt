package no.nav.etterlatte.libs.ktor

import io.ktor.http.auth.HttpAuthHeader
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.parseAuthorizationHeader
import io.ktor.server.auth.principal
import io.ktor.util.pipeline.PipelineContext
import no.nav.security.token.support.v2.TokenValidationContextPrincipal

inline val PipelineContext<*, ApplicationCall>.saksbehandler: String
    get() =
        call.principal<TokenValidationContextPrincipal>().let {
            val navIdent = it?.context?.getJwtToken("azure")
                ?.jwtTokenClaims?.getStringClaim("NAVident")
                ?: throw Exception("Navident is null in token, probably missing claim NAVident")
            navIdent
        }

inline val PipelineContext<*, ApplicationCall>.accesstoken: String
    get() = call.request.parseAuthorizationHeader().let {
        if (!(it == null || it !is HttpAuthHeader.Single || it.authScheme != "Bearer")) {
            it.blob
        } else {
            throw Exception("Missing authorization header")
        }
    }