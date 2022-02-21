package no.nav.etterlatte.libs.common.person

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.etterlatte.libs.common.person.Foedselsnummer


@JsonIgnoreProperties(ignoreUnknown = true)
data class HentPersonRequest(
    val foedselsnummer: Foedselsnummer,
    val historikk: Boolean = false,
    val utland: Boolean = false,
    val adresse: Boolean = false,
    val familieRelasjon: Boolean = false
)
