package no.nav.etterlatte.brev.distribusjon

import no.nav.etterlatte.libs.common.person.Adresse

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
    val adressetype: String,
    val adresselinje1: String?,
    val adresselinje2: String?,
    val adresselinje3: String?,
    val postnummer: String?,
    val poststed: String?,
    val land: String
)

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