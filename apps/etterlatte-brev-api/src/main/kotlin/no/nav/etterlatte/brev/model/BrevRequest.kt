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

        // todo: Må legge til støtte for utenlandske adresser. Er kun adresselinje 1 hvis innland. linje 2 og 3 hvis utland
        fun fraRegoppslag(regoppslag: RegoppslagResponseDTO) = MottakerRequest(
            navn = regoppslag.navn,
            adresse = """
                ${regoppslag.adresse.adresselinje1}, ${regoppslag.adresse.postnummer} ${regoppslag.adresse.poststed?.capitalize()}
            """.trimIndent(),
            postnummer = regoppslag.adresse.postnummer,
            poststed = regoppslag.adresse.poststed,
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