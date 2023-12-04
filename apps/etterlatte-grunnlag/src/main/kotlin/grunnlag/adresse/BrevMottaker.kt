package no.nav.etterlatte.grunnlag.adresse

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class BrevMottaker(
    val navn: String,
    val foedselsnummer: Foedselsnummer?,
    val adresse: Adresse,
)

data class Adresse(
    val adresseType: String,
    val adresselinje1: String? = null,
    val adresselinje2: String? = null,
    val adresselinje3: String? = null,
    val postnummer: String? = null,
    val poststed: String? = null,
    val landkode: String,
    val land: String,
)

data class Foedselsnummer(
    val value: String,
)
