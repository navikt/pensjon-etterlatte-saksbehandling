package no.nav.etterlatte.libs.common

import com.fasterxml.jackson.annotation.JsonValue

data class Enhetsnummer(
    @JsonValue val enhetNr: String,
) {
    companion object {
        val ukjent = Enhetsnummer("-001")

        // Person som ikke får bydel på geografisk område men kommunenr for Oslo(0301) som betyr ingen tilknytning mot noe lokalt navkontor
        val ingenTilknytning = Enhetsnummer("0000")

        fun nullable(enhetNr: String? = null) = enhetNr.takeUnless { it.isNullOrBlank() }?.let { Enhetsnummer(it) }
    }
}
