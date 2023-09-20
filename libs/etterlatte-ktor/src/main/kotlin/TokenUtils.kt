package no.nav.etterlatte.libs.ktor

import io.ktor.http.auth.HttpAuthHeader
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.parseAuthorizationHeader
import io.ktor.util.pipeline.PipelineContext
import no.nav.etterlatte.token.BrukerTokenInfo
import no.nav.etterlatte.token.Claims
import no.nav.security.token.support.core.jwt.JwtTokenClaims

const val AZURE_ISSUER = "azure"

fun hentAccessToken(call: ApplicationCall) = call.request.parseAuthorizationHeader().let {
    if (!(it == null || it !is HttpAuthHeader.Single || it.authScheme != "Bearer")) {
        it.blob
    } else {
        throw Exception("Missing authorization header")
    }
}

inline val PipelineContext<*, ApplicationCall>.brukerTokenInfo: BrukerTokenInfo
    get() {
        return call.brukerTokenInfo
    }

inline val ApplicationCall.brukerTokenInfo: BrukerTokenInfo
    get() {
        val claims = this.hentTokenClaims(AZURE_ISSUER)
        val oidSub = claims
            ?.let {
                val oid = it.getClaim(Claims.oid)
                val sub = it.getClaim(Claims.sub)
                Pair(oid, sub)
            }
        val saksbehandler = claims?.getClaim(Claims.NAVident)
        return BrukerTokenInfo.of(
            accessToken = hentAccessToken(this),
            oid = oidSub?.first,
            sub = oidSub?.second,
            saksbehandler = saksbehandler,
            claims = claims
        )
    }

fun JwtTokenClaims.getClaim(claim: Claims) = getStringClaim(claim.name)