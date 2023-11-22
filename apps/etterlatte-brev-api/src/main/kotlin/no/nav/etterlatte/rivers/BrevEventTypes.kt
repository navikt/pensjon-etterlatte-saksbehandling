package no.nav.etterlatte.rivers

enum class BrevEventTypes {
    FIKS_ENKELTBREV,
    OPPRETTET,
    FERDIGSTILT,
    JOURNALFOERT,
    DISTRIBUERT,
    ;

    override fun toString(): String {
        return "BREV:$name"
    }
}
