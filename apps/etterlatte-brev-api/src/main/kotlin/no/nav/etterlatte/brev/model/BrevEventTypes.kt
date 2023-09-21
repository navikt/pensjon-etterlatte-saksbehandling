package no.nav.etterlatte.brev.model

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
