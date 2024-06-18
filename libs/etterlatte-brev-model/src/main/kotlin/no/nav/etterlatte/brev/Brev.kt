package no.nav.etterlatte.brev.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.etterlatte.brev.Brevtype
import no.nav.etterlatte.libs.common.person.MottakerFoedselsnummer
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.util.UUID

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
    fun erGyldig(): Boolean =
        if (adresseType.isBlank() || landkode.isBlank() || land.isBlank()) {
            false
        } else if (adresseType == "NORSKPOSTADRESSE") {
            !(postnummer.isNullOrBlank() || poststed.isNullOrBlank())
        } else if (adresseType == "UTENLANDSKPOSTADRESSE") {
            !adresselinje1.isNullOrBlank()
        } else {
            true
        }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class Mottaker(
    val navn: String,
    val foedselsnummer: MottakerFoedselsnummer? = null,
    val orgnummer: String? = null,
    val adresse: Adresse,
) {
    fun erGyldig(): Boolean =
        if (navn.isBlank()) {
            false
        } else if ((foedselsnummer == null || foedselsnummer.value.isBlank()) && orgnummer.isNullOrBlank()) {
            false
        } else {
            adresse.erGyldig()
        }
}

data class Brev(
    val id: BrevID,
    val sakId: Long,
    val behandlingId: UUID?,
    val tittel: String?,
    val spraak: Spraak,
    val prosessType: BrevProsessType,
    val soekerFnr: String,
    val status: Status,
    val statusEndret: Tidspunkt,
    val opprettet: Tidspunkt,
    val mottaker: Mottaker,
    val brevtype: Brevtype,
) {
    fun kanEndres() = status in listOf(Status.OPPRETTET, Status.OPPDATERT)
}

enum class BrevProsessType {
    MANUELL,
    REDIGERBAR,
    AUTOMATISK,
    OPPLASTET_PDF,
}
