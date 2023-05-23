package no.nav.etterlatte.brev.model

import no.nav.pensjon.brevbaker.api.model.Telefonnummer

abstract class BrevData {
    abstract val avsender: Avsender
    abstract val mottaker: BrevMottaker
    abstract val attestant: Attestant?
    abstract fun templateName(): String
}

data class BrevMottaker(
    val navn: String,
    val adresse: String,
    val postnummer: String? = null,
    val poststed: String? = null,
    val land: String? = null
) {
    companion object {
        fun fra(mottaker: Mottaker) = BrevMottaker(
            navn = mottaker.navn,
            adresse = mottaker.adresse.let {
                listOfNotNull(
                    it.adresselinje1,
                    it.adresselinje2,
                    it.adresselinje3
                ).joinToString(", ")
            },
            postnummer = mottaker.adresse.postnummer,
            poststed = mottaker.adresse.poststed?.capitalize(),
            land = mottaker.adresse.land
        )
    }
}

fun String.capitalize(): String = this.lowercase().replaceFirstChar { char -> char.titlecase() }

data class Avsender(
    val kontor: String,
    val adresse: String,
    val postnummer: String,
    val telefonnummer: Telefonnummer,
    val saksbehandler: String
)

data class Attestant(
    val navn: String,
    val kontor: String
)