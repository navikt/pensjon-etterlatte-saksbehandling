package no.nav.etterlatte.brev.distribusjon

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue

// https://confluence.adeo.no/pages/viewpage.action?pageId=320038938
data class DistribuerJournalpostRequest(
    val journalpostId: String,
    val adresse: Adresse? = null,
    val bestillendeFagsystem: String,
    val dokumentProdApp: String,
    val distribusjonstype: DistribusjonsType,
    val distribusjonstidspunkt: DistribusjonsTidspunktType
)

data class Adresse(
    val adressetype: AdresseType,
    val adresselinje1: String?,
    val adresselinje2: String?,
    val adresselinje3: String?,
    val postnummer: String?,
    val poststed: String?,
    @JsonProperty("land")
    val landkode: String
) {
    init {
        if (adressetype == AdresseType.NORSK) {
            requireNotNull(postnummer)
            requireNotNull(poststed)
        } else if (adressetype == AdresseType.UTENLANDSK) {
            requireNotNull(adresselinje1)
        }
    }
}

enum class AdresseType(@JsonValue val verdi: String) {
    NORSK("norskPostadresse"),
    UTENLANDSK("utenlandskPostadresse");

    companion object {
        fun fra(s: String) = values().first { s.equals(it.verdi, ignoreCase = true) }
    }
}

enum class DistribusjonsType {
    VEDTAK,
    VIKTIG,
    ANNET
}

enum class DistribusjonsTidspunktType {
    UMIDDELBART, // Dokumentet distribueres så fort som mulig, uansett tidspunkt.
    KJERNETID // KJERNETID (07:00-23:00)
}

data class DistribuerJournalpostResponse(val bestillingsId: BestillingsID)
typealias BestillingsID = String