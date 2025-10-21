package no.nav.etterlatte.brev.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.etterlatte.brev.BrevParametereAutomatisk
import no.nav.etterlatte.brev.Brevkoder
import no.nav.etterlatte.brev.Brevtype
import no.nav.etterlatte.brev.SaksbehandlerOgAttestant
import no.nav.etterlatte.libs.common.Enhetsnummer
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
    UTGAATT,
    ;

    fun ikkeFerdigstilt(): Boolean = this in listOf(OPPRETTET, OPPDATERT)

    fun ikkeJournalfoert(): Boolean = this in listOf(OPPRETTET, OPPDATERT, FERDIGSTILT)

    fun ikkeDistribuert(): Boolean = this != DISTRIBUERT

    fun erDistribuert(): Boolean = this == DISTRIBUERT
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
            listOf(
                "Adressetype ($adresseType), landkode ($landkode) eller land ($land) er blank. Sjekk om det er verge i saken. Da vet vi ikke hvor brevet skal.",
            )
        } else if (adresseType == "NORSKPOSTADRESSE") {
            if (!(postnummer.isNullOrBlank() || poststed.isNullOrBlank())) {
                emptyList()
            } else {
                listOf("Postnummer eller poststed er ikke angitt")
            }
        } else if (adresseType == "UTENLANDSKPOSTADRESSE") {
            if (adresselinje1.isNullOrBlank()) {
                listOf("Adresselinje1 er ikke angitt")
            } else if (!postnummer.isNullOrBlank() || !poststed.isNullOrBlank()) {
                listOf("Postnummer og poststed skal ikke brukes på utenlandsk adresse")
            } else {
                emptyList()
            }
        } else {
            emptyList()
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
            listOf("Fødselsnummer og orgnummer er ikke angitt")
        } else {
            val erGyldig = adresse.erGyldig()
            if (erVerge()) {
                erGyldig.plus("Saksbehandler må manuelt håndtere verge").reversed()
            }
            erGyldig
        }

    fun erVerge() = this.navn == VERGENAVN_FOR_MOTTAKER
}

const val VERGENAVN_FOR_MOTTAKER = "Ukjent(Vergemål, kan ikke sende automatisk)"

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

    fun erFerdigstilt() = status in listOf(Status.FERDIGSTILT)

    fun erDistribuert() = status in listOf(Status.DISTRIBUERT)
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

data class GenererOgFerdigstillVedtaksbrev(
    val behandlingId: UUID,
    val brevId: BrevID,
)

data class FerdigstillJournalFoerOgDistribuerOpprettetBrev(
    val brevId: BrevID,
    val sakId: SakId,
    val enhetsnummer: Enhetsnummer,
    val avsenderRequest: SaksbehandlerOgAttestant,
)

data class BrevStatusResponse(
    val brevId: BrevID,
    val status: Status,
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

class Pdf(
    val bytes: ByteArray,
)

class PdfMedData(
    val brevId: BrevID,
    val bytes: ByteArray,
    val opprettet: Tidspunkt,
)

data class KanFerdigstilleBrevResponse(
    val kanFerdigstille: Boolean,
    val aarsak: String? = null,
)
