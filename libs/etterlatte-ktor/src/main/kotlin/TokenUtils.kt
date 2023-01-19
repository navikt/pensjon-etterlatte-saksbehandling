package no.nav.etterlatte.libs.ktor

import io.ktor.http.auth.HttpAuthHeader
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.parseAuthorizationHeader
import io.ktor.server.auth.principal
import io.ktor.util.pipeline.PipelineContext
import no.nav.security.token.support.v2.TokenValidationContextPrincipal

inline val PipelineContext<*, ApplicationCall>.saksbehandler: Saksbehandler
    get() = hentSaksbehandler(call)

fun hentSaksbehandler(call: ApplicationCall) = call.principal<TokenValidationContextPrincipal>().let {
    val navIdent = it?.context?.getJwtToken("azure")
        ?.jwtTokenClaims?.getStringClaim("NAVident")
        ?: throw Exception("Navident is null in token, probably missing claim NAVident")
    Saksbehandler(navIdent)
}

inline val PipelineContext<*, ApplicationCall>.accesstoken: String
    get() = call.request.parseAuthorizationHeader().let {
        if (!(it == null || it !is HttpAuthHeader.Single || it.authScheme != "Bearer")) {
            it.blob
        } else {
            throw Exception("Missing authorization header")
        }
    }

data class Saksbehandler(val ident: String)

data class SaksbehandlerProvider(val saksbehandler: (call: ApplicationCall) -> Saksbehandler) {
    fun invoke(call: ApplicationCall) = saksbehandler.invoke(call)
}