package no.nav.etterlatte.libs.ktor

import io.ktor.http.auth.HttpAuthHeader
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.parseAuthorizationHeader
import io.ktor.server.auth.principal
import io.ktor.util.pipeline.PipelineContext
import no.nav.etterlatte.libs.common.sak.Saksbehandler
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import no.nav.security.token.support.v2.TokenValidationContextPrincipal

inline val PipelineContext<*, ApplicationCall>.saksbehandler: Saksbehandler
    get() = hentSaksbehandler(call)

fun hentSaksbehandler(call: ApplicationCall) = call.principal<TokenValidationContextPrincipal>().let {
    val navIdent = it?.context?.getJwtToken("azure")
        ?.jwtTokenClaims?.getClaim(Claims.NAVident)
        ?: throw Exception("Navident is null in token, probably missing claim NAVident")
    Saksbehandler(navIdent)
}

inline val PipelineContext<*, ApplicationCall>.accesstoken: String
    get() = hentAccessToken(call)

fun hentAccessToken(call: ApplicationCall) = call.request.parseAuthorizationHeader().let {
    if (!(it == null || it !is HttpAuthHeader.Single || it.authScheme != "Bearer")) {
        it.blob
    } else {
        throw Exception("Missing authorization header")
    }
}

inline val PipelineContext<*, ApplicationCall>.accesstokenWrapper: AccessTokenWrapper
    get() {
        val oidSub = call.principal<TokenValidationContextPrincipal>().let {
            val claims = it?.context?.getJwtToken("azure")
                ?.jwtTokenClaims
            val oid = claims?.getClaim(Claims.oid)
            val sub = claims?.getClaim(Claims.sub)
            Pair(oid, sub)
        }
        return AccessTokenWrapper(accessToken = hentAccessToken(call), oid = oidSub.first, sub = oidSub.second)
    }

data class SaksbehandlerProvider(val saksbehandler: (call: ApplicationCall) -> Saksbehandler) {
    fun invoke(call: ApplicationCall) = saksbehandler.invoke(call)
}

data class AccessTokenWrapper(val accessToken: String, val oid: String?, val sub: String?)

enum class Claims {
    NAVident,
    oid, // ktlint-disable enum-entry-name-case
    sub // ktlint-disable enum-entry-name-case
}

fun JwtTokenClaims.getClaim(claim: Claims) = getStringClaim(claim.name)