package no.nav.etterlatte.libs.ktor

import io.ktor.http.auth.HttpAuthHeader
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.parseAuthorizationHeader
import io.ktor.util.pipeline.PipelineContext
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Claims
import no.nav.security.token.support.core.jwt.JwtTokenClaims

enum class Issuers(
    val issuerName: String,
) {
    AZURE("azure"),
    MASKINPORTEN("maskinporten"),
    TOKENX("tokenx"),
}

fun hentAccessToken(call: ApplicationCall) =
    call.request.parseAuthorizationHeader().let {
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
        val claims = this.hentTokenClaimsForIssuerName(Issuers.AZURE.issuerName)
        val oidSub =
            claims
                ?.let {
                    val oid = it.getClaimAsString(Claims.oid)
                    val sub = it.getClaimAsString(Claims.sub)
                    Pair(oid, sub)
                }
        val saksbehandler = claims?.getClaimAsString(Claims.NAVident)

        return BrukerTokenInfo.of(
            accessToken = hentAccessToken(this),
            oid = oidSub?.first,
            sub = oidSub?.second,
            saksbehandler = saksbehandler,
            claims = claims,
        )
    }

fun JwtTokenClaims.getClaimAsString(claim: Claims) = getStringClaim(claim.name)
