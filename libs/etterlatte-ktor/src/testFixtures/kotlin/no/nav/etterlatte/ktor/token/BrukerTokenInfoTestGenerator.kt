package no.nav.etterlatte.ktor.token

import no.nav.etterlatte.libs.ktor.token.APP
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Claims
import no.nav.etterlatte.libs.ktor.token.Saksbehandler
import no.nav.etterlatte.libs.ktor.token.Systembruker
import no.nav.etterlatte.libs.ktor.token.tokenMedClaims

fun simpleSaksbehandler(
    ident: String = "saksbehandler",
    claims: Map<Claims, Any?> = mapOf(),
): Saksbehandler =
    BrukerTokenInfo.of(
        accessToken = "token",
        saksbehandler = ident,
        claims = tokenMedClaims(claims),
        idtyp = null,
    ) as Saksbehandler

fun simpleAttestant(ident: String = "attestant") = BrukerTokenInfo.of("token", ident, null, null) as Saksbehandler

fun systembruker(claims: Map<Claims, Any?> = mapOf(Claims.azp_name to "systembruker1")): Systembruker =
    BrukerTokenInfo.of(
        accessToken = "token",
        saksbehandler = null,
        claims = tokenMedClaims(claims + mapOf(Claims.idtyp to APP)),
        idtyp = APP,
    ) as Systembruker
