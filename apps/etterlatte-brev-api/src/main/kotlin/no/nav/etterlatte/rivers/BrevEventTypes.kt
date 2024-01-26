package no.nav.etterlatte.rivers

enum class BrevEventTypes {
    OPPRETTET,
    JOURNALFOERT,
    DISTRIBUERT,
    ;

    fun toEventname() = "BREV:${this.name}"
}
