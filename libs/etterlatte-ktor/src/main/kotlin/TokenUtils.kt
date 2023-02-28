package no.nav.etterlatte.libs.ktor

import io.ktor.http.auth.HttpAuthHeader
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.parseAuthorizationHeader
import io.ktor.server.auth.principal
import io.ktor.util.pipeline.PipelineContext
import no.nav.etterlatte.token.Bruker
import no.nav.etterlatte.token.Claims
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import no.nav.security.token.support.v2.TokenValidationContextPrincipal

fun hentAccessToken(call: ApplicationCall) = call.request.parseAuthorizationHeader().let {
    if (!(it == null || it !is HttpAuthHeader.Single || it.authScheme != "Bearer")) {
        it.blob
    } else {
        throw Exception("Missing authorization header")
    }
}

inline val PipelineContext<*, ApplicationCall>.bruker: Bruker
    get() {
        val claims = call.principal<TokenValidationContextPrincipal>()
            ?.context
            ?.getJwtToken("azure")
            ?.jwtTokenClaims
        val oidSub = claims
            ?.let {
                val oid = it.getClaim(Claims.oid)
                val sub = it.getClaim(Claims.sub)
                Pair(oid, sub)
            }
        val saksbehandler = claims?.getClaim(Claims.NAVident)
        return Bruker.of(
            accessToken = hentAccessToken(call),
            oid = oidSub?.first,
            sub = oidSub?.second,
            saksbehandler = saksbehandler
        )
    }

inline val ApplicationCall.bruker: Bruker
    get() {
        val claims = this.principal<TokenValidationContextPrincipal>()
            ?.context
            ?.getJwtToken("azure")
            ?.jwtTokenClaims
        val oidSub = claims
            ?.let {
                val oid = it.getClaim(Claims.oid)
                val sub = it.getClaim(Claims.sub)
                Pair(oid, sub)
            }
        val saksbehandler = claims?.getClaim(Claims.NAVident)
        return Bruker.of(
            accessToken = hentAccessToken(this),
            oid = oidSub?.first,
            sub = oidSub?.second,
            saksbehandler = saksbehandler
        )
    }

fun JwtTokenClaims.getClaim(claim: Claims) = getStringClaim(claim.name)