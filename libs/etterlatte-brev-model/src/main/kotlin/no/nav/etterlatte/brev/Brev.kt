package no.nav.etterlatte.brev.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.etterlatte.brev.AvsenderRequest
import no.nav.etterlatte.brev.BrevDataRedigerbar
import no.nav.etterlatte.brev.Brevkoder
import no.nav.etterlatte.brev.Brevtype
import no.nav.etterlatte.libs.common.person.MottakerFoedselsnummer
import no.nav.etterlatte.libs.common.sak.SakId
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
    ;

    fun ikkeFerdigstilt(): Boolean = this in listOf(OPPRETTET, OPPDATERT)

    fun ikkeJournalfoert(): Boolean = this in listOf(OPPRETTET, OPPDATERT, FERDIGSTILT)

    fun ikkeDistribuert(): Boolean = this != DISTRIBUERT
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
    fun erGyldig(): List<String> =
        if (adresseType.isBlank() || landkode.isBlank() || land.isBlank()) {
            listOf("Adressetype ($adresseType), landkode ($landkode) eller land ($land) er blank")
        } else if (adresseType == "NORSKPOSTADRESSE") {
            if (!(postnummer.isNullOrBlank() || poststed.isNullOrBlank())) {
                listOf()
            } else {
                listOf("Postnummer eller poststed er null eller blank")
            }
        } else if (adresseType == "UTENLANDSKPOSTADRESSE") {
            if (!adresselinje1.isNullOrBlank()) {
                listOf()
            } else {
                listOf("Adresselinje1 er null eller blank")
            }
        } else {
            listOf()
        }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class Mottaker(
    val navn: String,
    val foedselsnummer: MottakerFoedselsnummer? = null,
    val orgnummer: String? = null,
    val adresse: Adresse,
    val tvingSentralPrint: Boolean = false,
) {
    fun erGyldig(): List<String> =
        if (navn.isBlank()) {
            listOf("Navn er blank")
        } else if ((foedselsnummer == null || foedselsnummer.value.isBlank()) && orgnummer.isNullOrBlank()) {
            listOf("Fødselsnummer og orgnummer er null eller blank")
        } else {
            adresse.erGyldig()
        }
}

data class Brev(
    val id: BrevID,
    val sakId: SakId,
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
    val brevkoder: Brevkoder?,
    val journalpostId: String? = null,
    val bestillingId: String? = null,
) {
    fun kanEndres() = status in listOf(Status.OPPRETTET, Status.OPPDATERT)
}

enum class BrevProsessType {
    @Deprecated(
        "Bruk heller redigerbar. " +
            "Det er noen brev i databasen som er oppretta med denne, så ikke slett statusen",
    )
    MANUELL,
    REDIGERBAR,
    AUTOMATISK,
    OPPLASTET_PDF,
}

class OpprettJournalfoerOgDistribuerRequest(
    val brevKode: Brevkoder,
    val brevDataRedigerbar: BrevDataRedigerbar,
    val avsenderRequest: AvsenderRequest,
    val brevkode: Brevkoder,
    val sakId: SakId,
)

class JournalfoerVedtaksbrevResponse(
    val brevId: BrevID,
    val opprett: OpprettJournalpostResponse,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpprettJournalpostResponse(
    val journalpostId: String,
    val journalpostferdigstilt: Boolean,
    val dokumenter: List<DokumentInfo> = emptyList(),
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class DokumentInfo(
        val dokumentInfoId: String,
    )
}
