package no.nav.etterlatte.ktor.token

import com.nimbusds.jwt.JWTClaimsSet
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Saksbehandler
import no.nav.etterlatte.libs.ktor.token.Systembruker
import no.nav.security.token.support.core.jwt.JwtTokenClaims

fun simpleSaksbehandler(
    ident: String = "saksbehandler",
    claims: Map<String, Any?> = mapOf(),
): Saksbehandler =
    BrukerTokenInfo.of(
        accessToken = "token",
        saksbehandler = ident,
        claims = extracted(claims),
        idtyp = null,
    ) as Saksbehandler

fun simpleAttestant(ident: String = "attestant") = BrukerTokenInfo.of("token", ident, null, null) as Saksbehandler

fun systembruker(claims: Map<String, Any?>): Systembruker =
    BrukerTokenInfo.of(
        accessToken = "token",
        saksbehandler = null,
        claims = extracted(claims),
        idtyp = "app",
    ) as Systembruker

private fun extracted(claims: Map<String, Any?>) =
    claims.entries
        .fold(JWTClaimsSet.Builder()) { acc, next ->
            acc.claim(next.key, next.value)
        }.build()
        .let { JwtTokenClaims(it) }
