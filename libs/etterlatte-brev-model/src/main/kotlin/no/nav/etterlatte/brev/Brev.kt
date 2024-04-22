package no.nav.etterlatte.brev.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

typealias BrevID = Long

enum class Status {
    OPPRETTET,
    OPPDATERT,
    FERDIGSTILT,
    JOURNALFOERT,
    DISTRIBUERT,
    SLETTET,
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class Adresse(
    val adresseType: String,
    val adresselinje1: String? = null,
    val adresselinje2: String? = null,
    val adresselinje3: String? = null,
    val postnummer: String? = null,
    val poststed: String? = null,
    val landkode: String,
    val land: String,
) {
    fun erGyldig(): Boolean {
        return if (adresseType.isBlank() || landkode.isBlank() || land.isBlank()) {
            false
        } else if (adresseType == "NORSKPOSTADRESSE") {
            !(postnummer.isNullOrBlank() || poststed.isNullOrBlank())
        } else if (adresseType == "UTENLANDSKPOSTADRESSE") {
            !adresselinje1.isNullOrBlank()
        } else {
            true
        }
    }
}

enum class BrevProsessType {
    MANUELL,
    REDIGERBAR,
    AUTOMATISK,
    OPPLASTET_PDF,
}
