package no.nav.etterlatte.brev.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.etterlatte.brev.adresse.RegoppslagResponseDTO
import no.nav.etterlatte.brev.distribusjon.BestillingsID
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.pensjon.brevbaker.api.model.Foedselsnummer
import org.apache.pdfbox.Loader
import org.apache.pdfbox.multipdf.PDFMergerUtility
import java.io.ByteArrayOutputStream
import java.time.LocalDate
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
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Mottaker(
    val navn: String,
    val foedselsnummer: Foedselsnummer? = null,
    val orgnummer: String? = null,
    val adresse: Adresse,
) {
    fun erGyldig(): Boolean {
        return if (navn.isBlank()) {
            false
        } else if ((foedselsnummer == null || foedselsnummer.value.isBlank()) && orgnummer.isNullOrBlank()) {
            false
        } else if (adresse.landkode.isBlank() || adresse.land.isBlank()) {
            false
        } else if (adresse.adresseType == "NORSKPOSTADRESSE") {
            !(adresse.postnummer.isNullOrBlank() || adresse.poststed.isNullOrBlank())
        } else if (adresse.adresseType == "UTENLANDSKPOSTADRESSE") {
            !adresse.adresselinje1.isNullOrBlank()
        } else {
            true
        }
    }

    companion object {
        fun fra(
            fnr: Folkeregisteridentifikator,
            regoppslag: RegoppslagResponseDTO,
        ) = Mottaker(
            navn = regoppslag.navn,
            foedselsnummer = Foedselsnummer(fnr.value),
            adresse =
                Adresse(
                    adresseType = regoppslag.adresse.type.name,
                    adresselinje1 = regoppslag.adresse.adresselinje1,
                    adresselinje2 = regoppslag.adresse.adresselinje2,
                    adresselinje3 = regoppslag.adresse.adresselinje3,
                    postnummer = regoppslag.adresse.postnummer,
                    poststed = regoppslag.adresse.poststed,
                    landkode = regoppslag.adresse.landkode,
                    land = regoppslag.adresse.land,
                ),
        )

        fun tom(fnr: Folkeregisteridentifikator) =
            Mottaker(
                navn = "N/A",
                foedselsnummer = Foedselsnummer(fnr.value),
                adresse =
                    Adresse(
                        adresseType = "",
                        landkode = "",
                        land = "",
                    ),
            )
    }
}

data class Brev(
    val id: BrevID,
    val sakId: Long,
    val behandlingId: UUID?,
    val tittel: String?,
    val prosessType: BrevProsessType,
    val soekerFnr: String,
    val status: Status,
    val statusEndret: Tidspunkt,
    val opprettet: Tidspunkt,
    val mottaker: Mottaker,
    val journalpostId: String? = null,
    val bestillingsID: BestillingsID? = null,
    val brevtype: Brevtype,
) {
    fun kanEndres() = status in listOf(Status.OPPRETTET, Status.OPPDATERT)

    companion object {
        fun fra(
            id: BrevID,
            opprettNyttBrev: OpprettNyttBrev,
        ) = Brev(
            id = id,
            sakId = opprettNyttBrev.sakId,
            behandlingId = opprettNyttBrev.behandlingId,
            tittel = opprettNyttBrev.innhold.tittel,
            prosessType = opprettNyttBrev.prosessType,
            soekerFnr = opprettNyttBrev.soekerFnr,
            status = opprettNyttBrev.status,
            statusEndret = opprettNyttBrev.opprettet,
            mottaker = opprettNyttBrev.mottaker,
            opprettet = opprettNyttBrev.opprettet,
            brevtype = opprettNyttBrev.brevtype,
        )
    }
}

class Pdf(val bytes: ByteArray) {
    fun medPdfAppended(pdf: Pdf): Pdf {
        val foerstePdf = Loader.loadPDF(this.bytes)
        val andrePdf = Loader.loadPDF(pdf.bytes)
        PDFMergerUtility().appendDocument(foerstePdf, andrePdf)

        val out = ByteArrayOutputStream()
        foerstePdf.save(out)
        return Pdf(out.toByteArray())
    }
}

data class BrevInnhold(
    val tittel: String,
    val spraak: Spraak,
    val payload: Slate? = null,
)

data class BrevInnholdVedlegg(
    val tittel: String,
    val key: BrevVedleggKey,
    val payload: Slate? = null,
)

enum class BrevVedleggKey {
    OMS_BEREGNING,
    OMS_FORHAANDSVARSEL_FEILUTBETALING,
    BP_BEREGNING_TRYGDETID,
}

data class OpprettNyttBrev(
    val sakId: Long,
    val behandlingId: UUID?,
    val soekerFnr: String,
    val prosessType: BrevProsessType,
    val mottaker: Mottaker,
    val opprettet: Tidspunkt,
    val innhold: BrevInnhold,
    val innholdVedlegg: List<BrevInnholdVedlegg>?,
    val brevtype: Brevtype,
) {
    val status: Status = Status.OPPRETTET
}

enum class BrevProsessType {
    MANUELL,
    REDIGERBAR,
    AUTOMATISK,
    OPPLASTET_PDF,
}

data class EtterbetalingDTO(
    val datoFom: LocalDate,
    val datoTom: LocalDate,
)

enum class Brevtype {
    VEDTAK,
    VARSEL,
    INFORMASJON,
    OPPLASTET_PDF,
    MANUELT,
    VEDLEGG,
}
