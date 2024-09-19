package no.nav.etterlatte.behandling.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.etterlatte.libs.common.Enhetsnummer

@JsonIgnoreProperties(ignoreUnknown = true)
data class Navkontor(
    val navn: String,
    val enhetNr: Enhetsnummer,
)
