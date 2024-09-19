package no.nav.etterlatte.libs.common

import com.fasterxml.jackson.annotation.JsonValue

data class Enhetsnummer(
    @JsonValue val enhetNr: String,
)
