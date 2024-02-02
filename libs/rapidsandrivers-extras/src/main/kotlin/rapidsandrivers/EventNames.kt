package no.nav.etterlatte.rapidsandrivers

import no.nav.etterlatte.libs.common.event.EventnameHendelseType

enum class EventNames(val eventname: String) : EventnameHendelseType {
    GRUNNLAGSVERSJONERING_EVENT_NAME("GRUNNLAGSVERSJONERING_EVENT_NAME"),
    FEILA("FEILA"),
    FORDELER_STATISTIKK("FORDELER:STATISTIKK"),
    ALDERSOVERANG("ALDERSOVERANG"),
    ;

    override fun lagEventnameForType(): String = this.eventname
}
