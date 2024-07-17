package no.nav.etterlatte.ktor.token

import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Saksbehandler

fun simpleSaksbehandler(ident: String = "saksbehandler") = BrukerTokenInfo.of("token", ident, null, null) as Saksbehandler

fun simpleAttestant(ident: String = "attestant") = BrukerTokenInfo.of("token", ident, null, null) as Saksbehandler
