package no.nav.etterlatte.rivers

enum class BrevEventTypes {
    OPPRETTET,
    PDF_GENERERT,
    FERDIGSTILT,
    JOURNALFOERT,
    DISTRIBUERT,
    ;

    override fun toString(): String {
        return "BREV:$name"
    }
}
