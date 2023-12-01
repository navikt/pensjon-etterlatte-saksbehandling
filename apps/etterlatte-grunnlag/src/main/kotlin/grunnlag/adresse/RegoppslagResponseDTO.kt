package no.nav.etterlatte.grunnlag.adresse

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class RegoppslagResponseDTO(
    val navn: String,
    val adresse: Adresse,
) {
    data class Adresse(
        val type: AdresseType,
        val adresseKilde: AdresseKilde?,
        val adresselinje1: String,
        val adresselinje2: String?,
        val adresselinje3: String?,
        val postnummer: String?,
        val poststed: String?,
        val landkode: String,
        val land: String,
    )

    enum class AdresseType {
        NORSKPOSTADRESSE,
        UTENLANDSKPOSTADRESSE,
    }

    enum class AdresseKilde {
        BOSTEDSADRESSE,
        OPPHOLDSADRESSE,
        KONTAKTADRESSE,
        DELTBOSTED,
        KONTAKTINFORMASJONFORDØDSBO,
        ENHETPOSTADRESSE,
        ENHETFORRETNINGSADRESSE,
    }
}
