package no.nav.etterlatte.libs.ktor

import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.principal
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import no.nav.security.token.support.v2.TokenValidationContextPrincipal

fun ApplicationCall.hentTokenClaims(issuerName: String): JwtTokenClaims? =
    this.principal<TokenValidationContextPrincipal>()
        ?.context
        ?.let { it.hentTokenClaims(issuerName) }

fun TokenValidationContext?.hentTokenClaims(issuerName: String): JwtTokenClaims? = this
    ?.getJwtToken(issuerName)
    ?.jwtTokenClaims

fun ApplicationCall.firstValidTokenClaims(): JwtTokenClaims? = this
    .principal<TokenValidationContextPrincipal>()
    ?.context
    ?.firstValidToken
    ?.get()
    ?.jwtTokenClaims