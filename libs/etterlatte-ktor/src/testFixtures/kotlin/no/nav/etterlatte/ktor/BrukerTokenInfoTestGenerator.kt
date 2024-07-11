package no.nav.etterlatte.ktor

import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo

fun simpleSaksbehandler() = BrukerTokenInfo.of("token", "saksbehandler", null, null)

fun simpleSaksbehandlerMedIdent(ident: String) = BrukerTokenInfo.of("token", ident, null, null)

fun simpleAttestant() = BrukerTokenInfo.of("token", "attestant", null, null)
