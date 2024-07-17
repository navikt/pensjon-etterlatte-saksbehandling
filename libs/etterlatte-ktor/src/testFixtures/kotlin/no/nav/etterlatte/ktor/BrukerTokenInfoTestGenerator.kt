package no.nav.etterlatte.ktor

import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo

fun simpleSaksbehandler(ident: String = "saksbehandler") = BrukerTokenInfo.of("token", ident, null, null)

fun simpleAttestant() = BrukerTokenInfo.of("token", "attestant", null, null)
