package no.nav.etterlatte.libs.common.kodeverk

data class LandDto(
    val isoLandkode: String,
    val gyldigFra: String,
    val gyldigTil: String,
    val beskrivelse: BeskrivelseDto,
)

data class BeskrivelseDto(
    val term: String,
    val tekst: String,
)
