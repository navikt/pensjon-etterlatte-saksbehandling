package no.nav.etterlatte.brev.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.etterlatte.libs.common.brev.model.Adresse
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Spraak
import java.time.LocalDate

abstract class BrevRequest {
    abstract val spraak: Spraak
    abstract val avsender: Avsender
    abstract val mottaker: Mottaker
    abstract val attestant: Attestant
    val utsendingsDato = LocalDate.now()
    abstract fun templateName(): String
}

// TODO: Sikre non-nullable
data class Mottaker(
    val navn: String? = null,
    val adresse: String? = null,
    val postnummer: String? = null,
    val poststed: String? = null,
    val land: String? = null
) {
    companion object {
        fun fraAdresse(adresse: Adresse) = Mottaker(
            navn = "${adresse.fornavn} ${adresse.etternavn}",
            adresse = adresse.adresse,
            postnummer = adresse.postnummer,
            poststed = adresse.poststed,
            land = adresse.land
        )

        // todo: Må legge til støtte for utenlandske adresser. Er kun adresselinje 1 hvis innland. linje 2 og 3 hvis utland
        fun fraRegoppslag(regoppslag: RegoppslagResponseDTO) = Mottaker(
            navn = regoppslag.navn,
            adresse = regoppslag.adresse.adresselinje1,
            postnummer = regoppslag.adresse.postnummer,
            poststed = regoppslag.adresse.poststed,
            land = regoppslag.adresse.land
        )
    }
}

data class Avsender(
    val kontor: String,
    val adresse: String,
    val postnummer: String,
    val telefon: String,
    val saksbehandler: String
)

data class Attestant(
    val navn: String,
    val kontor: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class RegoppslagResponseDTO(
    val navn: String,
    val adresse: Adresse
) {
    data class Adresse(
        val type: AdresseType,
        val adresseKilde: AdresseKilde,
        val adresselinje1: String,
        val adresselinje2: String?,
        val adresselinje3: String?,
        val postnummer: String?,
        val poststed: String?,
        val landkode: String,
        val land: String
    )

    enum class AdresseType {
        NORSKPOSTADRESSE, UTENLANDSKPOSTADRESSE
    }

    enum class AdresseKilde {
        BOSTEDSADRESSE, OPPHOLDSADRESSE, KONTAKTADRESSE, DELTBOSTED, KONTAKTINFORMASJONFORDØDSBO, ENHETPOSTADRESSE, ENHETFORRETNINGSADRESSE
    }
}

data class SaksbehandlerInfo(
    val ident: String,
    val navn: String,
    val fornavn: String,
    val etternavn: String,
    val epost: String
)