package no.nav.etterlatte.libs.ktor.token

import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.principal
import no.nav.etterlatte.libs.ktor.Issuer
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import no.nav.security.token.support.v2.TokenValidationContextPrincipal

fun ApplicationCall.hentTokenClaimsForIssuerName(issuer: Issuer): JwtTokenClaims? =
    this
        .principal<TokenValidationContextPrincipal>()
        ?.context
        ?.let { it.hentTokenClaimsForIssuerName(issuer) }

fun TokenValidationContext?.hentTokenClaimsForIssuerName(issuer: Issuer): JwtTokenClaims? =
    this
        ?.getJwtToken(issuer.issuerName)
        ?.jwtTokenClaims

fun ApplicationCall.firstValidTokenClaims(): JwtTokenClaims? =
    this
        .principal<TokenValidationContextPrincipal>()
        ?.context
        ?.firstValidToken
        ?.jwtTokenClaims
