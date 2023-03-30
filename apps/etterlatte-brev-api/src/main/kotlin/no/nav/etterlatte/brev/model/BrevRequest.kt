package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.adresse.RegoppslagResponseDTO
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Spraak
import java.time.LocalDate

abstract class BrevRequest {
    abstract val spraak: Spraak
    abstract val avsender: Avsender
    abstract val mottaker: MottakerRequest
    abstract val attestant: Attestant?
    val utsendingsDato = LocalDate.now()
    abstract fun templateName(): String
}

// TODO: Sikre non-nullable
data class MottakerRequest(
    val navn: String? = null,
    val adresse: String? = null,
    val postnummer: String? = null,
    val poststed: String? = null,
    val land: String? = null
) {
    companion object {
        fun fraAdresse(adresse: Adresse) = MottakerRequest(
            navn = adresse.navn,
            adresse = adresse.adresse,
            postnummer = adresse.postnummer,
            poststed = adresse.poststed,
            land = adresse.land
        )

        fun fraRegoppslag(regoppslag: RegoppslagResponseDTO) = MottakerRequest(
            navn = regoppslag.navn,
            adresse = regoppslag.adresse.let {
                listOfNotNull(
                    it.adresselinje1,
                    it.adresselinje2,
                    it.adresselinje3
                ).joinToString(", ")
            },
            postnummer = regoppslag.adresse.postnummer,
            poststed = regoppslag.adresse.poststed?.capitalize(),
            land = regoppslag.adresse.land
        )
    }
}

fun String.capitalize(): String = this.lowercase().replaceFirstChar { char -> char.titlecase() }

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