package no.nav.etterlatte.person.pdl

import java.time.LocalDateTime

data class Bostedsadresse(
        val gyldigFraOgMed: LocalDateTime? = null,
        val gyldigTilOgMed: LocalDateTime? = null,
        val vegadresse: Vegadresse? = null,
        val utenlandskAdresse: UtenlandskAdresse? = null,
        val metadata: Metadata
)

data class Vegadresse(
        val adressenavn: String? = null,
        val husnummer: String? = null,
        val husbokstav: String? = null,
        val postnummer: String? = null,
        val kommunenummer: String? = null,
        val bydelsnummer: String? = null
)

data class UtenlandskAdresse(
        val adressenavnNummer: String? = null,
        val bySted: String? = null,
        val bygningEtasjeLeilighet: String? = null,
        val landkode: String,
        val postboksNummerNavn: String? = null,
        val postkode: String? = null,
        val regionDistriktOmraade: String? = null
)
