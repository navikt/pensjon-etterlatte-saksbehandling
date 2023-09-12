package no.nav.etterlatte.brev.journalpost

data class JournalpostRequest(
    val tittel: String,
    val journalpostType: JournalPostType,
    val tema: String,
    val kanal: String?,
    val journalfoerendeEnhet: String?,
    val avsenderMottaker: AvsenderMottaker,
    val bruker: Bruker,
    val sak: JournalpostSak,
    val eksternReferanseId: String,
    val dokumenter: List<JournalpostDokument>
)

data class AvsenderMottaker(
    val id: String?,
    val idType: String? = "FNR",
    val navn: String? = null,
    val land: String? = null
)

data class Bruker(
    val id: String,
    val idType: String = "FNR"
)

data class JournalpostDokument(
    val tittel: String,
    val dokumentKategori: DokumentKategori? = null, // depricated
    val brevkode: String = "XX.YY-ZZ",
    val dokumentvarianter: List<DokumentVariant>
)

data class JournalpostSak(
    val sakstype: Sakstype,
    val fagsakId: String? = null,
    val fagsaksystem: String? = "EY"
)

enum class Sakstype {
    FAGSAK,
    GENERELL_SAK
}

sealed class DokumentVariant {
    abstract val filtype: String
    abstract val fysiskDokument: String
    abstract val variantformat: String

    data class ArkivPDF(
        override val fysiskDokument: String
    ) : DokumentVariant() {
        override val filtype: String = "PDFA"
        override val variantformat: String = "ARKIV"
    }

    data class OriginalJson(
        override val fysiskDokument: String
    ) : DokumentVariant() {
        override val filtype: String = "JSON"
        override val variantformat: String = "ORIGINAL"
    }
}

enum class JournalPostType(val type: String) {
    INNGAAENDE("INNGAAENDE"),
    UTGAAENDE("UTGAAENDE")
}

enum class DokumentKategori(val type: String) {
    SOK("SOK"),
    VB("VB"),
    IB("IB")
}

enum class BrukerIdType {
    FNR,
    AKTOERID,
    ORGNR
}

class JournalpostKoder {
    companion object {
        const val BREV_KODE = "XX.YY-ZZ"
    }
}

data class FerdigstillJournalpostRequest(
    val journalfoerendeEnhet: String
)