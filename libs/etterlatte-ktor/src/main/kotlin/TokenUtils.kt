package no.nav.etterlatte.libs.ktor

import io.ktor.http.auth.HttpAuthHeader
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.parseAuthorizationHeader
import io.ktor.server.auth.principal
import io.ktor.util.pipeline.PipelineContext
import no.nav.etterlatte.token.AccessTokenWrapper
import no.nav.etterlatte.token.Claims
import no.nav.etterlatte.token.Saksbehandler
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import no.nav.security.token.support.v2.TokenValidationContextPrincipal

@Deprecated(
    "gå heller via accesstokenWrapper for å få støtte for også automatiske behandlinger",
    ReplaceWith("accesstokenWrapper")
)
inline val PipelineContext<*, ApplicationCall>.saksbehandler: Saksbehandler
    get() = hentSaksbehandler(call)

fun hentSaksbehandler(call: ApplicationCall) = call.principal<TokenValidationContextPrincipal>().let {
    val navIdent = it?.context?.getJwtToken("azure")
        ?.jwtTokenClaims?.getClaim(Claims.NAVident)
        ?: throw Exception("Navident is null in token, probably missing claim NAVident")
    Saksbehandler(navIdent)
}

@Deprecated("bruk heller accesstokenWrapper", ReplaceWith("accesstokenWrapper"))
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
            ?.let { Saksbehandler(it) }
        return AccessTokenWrapper(
            accessToken = hentAccessToken(call),
            oid = oidSub?.first,
            sub = oidSub?.second,
            saksbehandler = saksbehandler
        )
    }

fun JwtTokenClaims.getClaim(claim: Claims) = getStringClaim(claim.name)