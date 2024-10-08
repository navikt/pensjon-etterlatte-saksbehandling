package no.nav.etterlatte.libs.ktor.token

import no.nav.etterlatte.libs.common.Enhetsnummer

enum class Fagsaksystem(
    val navn: String,
    val enhet: Enhetsnummer,
    val systemnavn: String,
) {
    EY("EY", Enhetsnummer.ingenTilknytning, "Gjenny"),
}
