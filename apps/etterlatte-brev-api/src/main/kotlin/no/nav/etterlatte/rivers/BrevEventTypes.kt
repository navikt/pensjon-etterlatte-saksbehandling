package no.nav.etterlatte.rivers

import no.nav.etterlatte.event.EventName

enum class BrevEventTypes : EventName {
    OPPRETTET,
    FERDIGSTILT,
    JOURNALFOERT,
    DISTRIBUERT,
    ;

    override fun toEventName(): String {
        return "BREV:$name"
    }
}
