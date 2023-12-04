package no.nav.etterlatte.grunnlag.adresse

data class RegoppslagAdresse(
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

    @Suppress("unused")
    UTENLANDSKPOSTADRESSE,
}

enum class AdresseKilde {
    BOSTEDSADRESSE,

    @Suppress("unused")
    OPPHOLDSADRESSE,

    @Suppress("unused")
    KONTAKTADRESSE,

    @Suppress("unused")
    DELTBOSTED,

    @Suppress("unused")
    KONTAKTINFORMASJONFORDØDSBO,

    @Suppress("unused")
    ENHETPOSTADRESSE,

    @Suppress("unused")
    ENHETFORRETNINGSADRESSE,
}
