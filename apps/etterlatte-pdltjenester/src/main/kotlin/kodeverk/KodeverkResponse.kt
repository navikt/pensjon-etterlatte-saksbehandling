package no.nav.etterlatte.kodeverk

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

data class Beskrivelse(val term: String, val tekst: String)

data class Betydning(val gyldigFra: String, val gyldigTil: String, val beskrivelser: Map<String, Beskrivelse>)

@JsonIgnoreProperties(ignoreUnknown = true)
data class KodeverkResponse(val betydninger: Map<String, List<Betydning>>)
