package no.nav.etterlatte.brev.model

data class Land(
    val isoLandkode: String,
    val beskrivelse: Beskrivelse,
)

data class Beskrivelse(
    val tekst: String,
)
