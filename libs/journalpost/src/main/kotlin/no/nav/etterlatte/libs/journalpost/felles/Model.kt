package no.nav.etterlatte.libs.journalpost.felles

data class AvsenderMottaker(
    val id: String?,
    val idType: BrukerIdType? = BrukerIdType.FNR,
    val navn: String? = null,
    val land: String? = null,
)

data class Bruker(
    val id: String,
    val idType: BrukerIdType = BrukerIdType.FNR,
)

data class JournalpostSak(
    val sakstype: Sakstype,
    val fagsakId: String? = null,
    val tema: String? = null,
    val fagsaksystem: String? = null,
) {
    init {
        if (sakstype == Sakstype.FAGSAK) {
            check(!fagsakId.isNullOrBlank()) { "fagsakId må være satt når sakstype=${Sakstype.FAGSAK}" }
            check(!fagsaksystem.isNullOrBlank()) { "fagsaksystem må være satt når sakstype=${Sakstype.FAGSAK}" }
        }
    }
}

enum class Sakstype {
    FAGSAK,
    GENERELL_SAK,
}

enum class JournalpostType(
    val type: String,
) {
    NOTAT("NOTAT"),
    INNGAAENDE("INNGAAENDE"),
    UTGAAENDE("UTGAAENDE"),
}

enum class BrukerIdType {
    FNR,
    AKTOERID,
    ORGNR,
}
