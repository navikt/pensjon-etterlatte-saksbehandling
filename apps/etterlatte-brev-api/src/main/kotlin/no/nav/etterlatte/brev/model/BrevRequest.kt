package no.nav.etterlatte.brev.model

import no.nav.etterlatte.libs.common.brev.model.Adresse
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Spraak
import java.time.LocalDate

abstract class BrevRequest {
    abstract val spraak: Spraak
    abstract val avsender: Avsender
    abstract val mottaker: Mottaker
    val utsendingsDato = LocalDate.now()
    abstract fun templateName(): String
}

// TODO: Sikre non-nullable
data class Mottaker(
    val fornavn: String? = null,
    val etternavn: String? = null,
    val adresse: String? = null,
    val postnummer: String? = null,
    val poststed: String? = null,
    val land: String? = null
) {
    companion object {
        fun fraAdresse(adresse: Adresse) = Mottaker(
            fornavn = adresse.fornavn,
            etternavn = adresse.etternavn,
            adresse = adresse.adresse,
            postnummer = adresse.postnummer,
            poststed = adresse.poststed,
            land = adresse.land
        )

        fun fraRegoppslag(regoppslag: RegoppslagResponseDTO) = Mottaker(
            fornavn = regoppslag.navn,
            etternavn = regoppslag.navn,
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
    val telefon: String
)

data class RegoppslagResponseDTO(
    val navn: String,
    val adresse: Adresse
) {
    data class Adresse(
        val type: AdresseType,
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
}