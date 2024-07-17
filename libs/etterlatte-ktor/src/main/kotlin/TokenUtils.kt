package no.nav.etterlatte.libs.ktor

import io.ktor.http.auth.HttpAuthHeader
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.parseAuthorizationHeader
import io.ktor.util.pipeline.PipelineContext
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Claims
import no.nav.etterlatte.libs.ktor.token.hentTokenClaimsForIssuerName
import no.nav.security.token.support.core.jwt.JwtTokenClaims

enum class Issuer(
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
        val claims = this.hentTokenClaimsForIssuerName(Issuer.AZURE)
        val saksbehandler = claims?.getClaimAsString(Claims.NAVident)
        val idtyp = claims?.getClaimAsString(Claims.idtyp)
        return BrukerTokenInfo.of(
            accessToken = hentAccessToken(this),
            saksbehandler = saksbehandler,
            idtyp = idtyp,
            claims = claims,
        )
    }

fun JwtTokenClaims.getClaimAsString(claim: Claims) = getStringClaim(claim.name)
