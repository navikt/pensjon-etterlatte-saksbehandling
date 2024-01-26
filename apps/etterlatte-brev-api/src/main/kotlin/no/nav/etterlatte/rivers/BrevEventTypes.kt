package no.nav.etterlatte.rivers

enum class BrevEventTypes {
    FIKS_ENKELTBREV,
    OPPRETTET,
    JOURNALFOERT,
    DISTRIBUERT,
    ;

    fun toEventname() = "BREV:${this.name}"
}
