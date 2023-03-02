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

inline val PipelineContext<*, ApplicationCall>.claims: JwtTokenClaims?
    get() = call.principal<TokenValidationContextPrincipal>()
        ?.context
        ?.getJwtToken("azure")
        ?.jwtTokenClaims

inline val PipelineContext<*, ApplicationCall>.bruker: Bruker
    get() {
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

enum class Group(val key: String) {
    SAKSBEHANDLER("AZUREAD_SAKSBEHANDLER_GROUPID"),
    ATTESTANT("AZUREAD_SAKSBEHANDLER_GROUPID")
}

fun PipelineContext<*, ApplicationCall>.harGruppetilgang(group: Group): Boolean =
    claims?.containsClaim("groups", Tilgangsgrupper.get()[group]) ?: false

fun JwtTokenClaims.getClaim(claim: Claims): String? = getStringClaim(claim.name)