package no.nav.etterlatte.behandling.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

data class ArbeidsFordelingRequest(
    val tema: String,
    val geografiskOmraade: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ArbeidsFordelingEnhet(
    val navn: String,
    val enhetNr: String
)