package no.nav.etterlatte.libs.journalpost.felles

data class JournalpostDokument(
    val tittel: String,
    val brevkode: String = JournalpostKoder.BREV_KODE,
    val dokumentvarianter: List<DokumentVariant>,
    val dokumentKategori: DokumentKategori = DokumentKategori.SOK,
)

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

enum class DokumentKategori(
    val type: String,
) {
    SOK("SOK"),
    VB("VB"),
    IB("IB"),
}

object JournalpostKoder {
    const val BREV_KODE = "XX.YY-ZZ"
}
