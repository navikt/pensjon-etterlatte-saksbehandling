package no.nav.etterlatte.behandling.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Navkontor(
    val navn: String,
    val enhetNr: String,
)
