package no.nav.etterlatte.libs.ktor.token

import io.ktor.http.auth.HttpAuthHeader
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.PipelineCall
import io.ktor.server.application.call
import io.ktor.server.auth.parseAuthorizationHeader
import io.ktor.server.routing.RoutingContext
import io.ktor.util.pipeline.PipelineContext
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

inline val PipelineContext<Unit, PipelineCall>.brukerTokenInfo: BrukerTokenInfo
    get() {
        return call.brukerTokenInfo
    }

inline val RoutingContext.brukerTokenInfo: BrukerTokenInfo
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
