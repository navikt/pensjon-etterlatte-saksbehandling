package no.nav.etterlatte.brev.dokarkiv

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.etterlatte.token.Fagsaksystem
import java.time.LocalDateTime

/**
 * Requestobjekt for å opprette ny Journalpost
 **/
@JsonInclude(JsonInclude.Include.NON_EMPTY)
data class OpprettJournalpostRequest(
    val avsenderMottaker: AvsenderMottaker,
    val behandlingstema: String? = null,
    val bruker: Bruker,
    val datoDokument: LocalDateTime? = null,
    val datoMottatt: LocalDateTime? = null,
    val dokumenter: List<JournalpostDokument>,
    val eksternReferanseId: String,
    val journalfoerendeEnhet: String,
    val journalposttype: JournalPostType,
    val kanal: String,
    val sak: JournalpostSak,
    val tema: String,
    val tilleggsopplysninger: Map<String, String> = emptyMap(),
    val tittel: String,
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

data class OppdaterJournalpostResponse(
    val journalpostId: String,
)

data class AvsenderMottaker(
    val id: String?,
    val idType: String? = "FNR",
    val navn: String? = null,
    val land: String? = null,
)

data class Bruker(
    val id: String,
    val idType: BrukerIdType = BrukerIdType.FNR,
)

data class JournalpostDokument(
    val tittel: String,
    val brevkode: String = "XX.YY-ZZ",
    val dokumentvarianter: List<DokumentVariant>,
)

data class JournalpostSak(
    val sakstype: Sakstype,
    val fagsakId: String? = null,
    val tema: String? = null,
) {
    val fagsaksystem: String = Fagsaksystem.EY.navn
}

enum class Sakstype {
    FAGSAK,
    GENERELL_SAK,
}

sealed class DokumentVariant {
    abstract val filtype: String
    abstract val fysiskDokument: String
    abstract val variantformat: String

    data class ArkivPDF(
        override val fysiskDokument: String,
    ) : DokumentVariant() {
        override val filtype: String = "PDFA"
        override val variantformat: String = "ARKIV"
    }

    data class OriginalJson(
        override val fysiskDokument: String,
    ) : DokumentVariant() {
        override val filtype: String = "JSON"
        override val variantformat: String = "ORIGINAL"
    }
}

enum class JournalPostType(val type: String) {
    INNGAAENDE("INNGAAENDE"),
    UTGAAENDE("UTGAAENDE"),
}

enum class DokumentKategori(val type: String) {
    SOK("SOK"),
    VB("VB"),
    IB("IB"),
}

enum class BrukerIdType {
    FNR,
    AKTOERID,
    ORGNR,
}

class JournalpostKoder {
    companion object {
        const val BREV_KODE = "XX.YY-ZZ"
    }
}
