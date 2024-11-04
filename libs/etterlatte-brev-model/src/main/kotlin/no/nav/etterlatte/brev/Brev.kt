package no.nav.etterlatte.brev.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.etterlatte.brev.BrevParametereAutomatisk
import no.nav.etterlatte.brev.Brevkoder
import no.nav.etterlatte.brev.Brevtype
import no.nav.etterlatte.brev.SaksbehandlerOgAttestant
import no.nav.etterlatte.libs.common.dbutils.Tidspunkt
import no.nav.etterlatte.libs.common.person.MottakerFoedselsnummer
import no.nav.etterlatte.libs.common.sak.SakId
import java.util.UUID

typealias BrevID = Long

enum class Status {
    OPPRETTET,
    OPPDATERT,
    FERDIGSTILT,
    JOURNALFOERT,
    DISTRIBUERT,
    SLETTET,
    UTGAATT,
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
    val id: UUID,
    val navn: String,
    val foedselsnummer: MottakerFoedselsnummer? = null,
    val orgnummer: String? = null,
    val adresse: Adresse,
    val tvingSentralPrint: Boolean = false,
    val type: MottakerType = MottakerType.HOVED,
    val journalpostId: String? = null,
    val bestillingId: String? = null,
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

enum class MottakerType { HOVED, KOPI }

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
    val mottakere: List<Mottaker>,
    val brevtype: Brevtype,
    val brevkoder: Brevkoder?,
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

data class OpprettJournalfoerOgDistribuerRequest(
    val brevKode: Brevkoder,
    val brevParametereAutomatisk: BrevParametereAutomatisk,
    val avsenderRequest: SaksbehandlerOgAttestant,
    val sakId: SakId,
)

data class JournalfoerVedtaksbrevResponseOgBrevid(
    val brevId: BrevID,
    val opprettetJournalpost: List<OpprettJournalpostResponse>,
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

data class BrevDistribusjonResponse(
    val brevId: BrevID,
    val erDistribuert: Boolean,
)
