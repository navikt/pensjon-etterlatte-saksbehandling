package no.nav.etterlatte.rivers

enum class BrevEventTypes {
    OPPRETTET,
    FERDIGSTILT,
    JOURNALFOERT,
    DISTRIBUERT,
    ;

    override fun toString(): String {
        return "BREV:$name"
    }
}
