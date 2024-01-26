package no.nav.etterlatte.rapidsandrivers

import no.nav.etterlatte.libs.common.event.EventnameHendelseType

enum class EventNames : EventnameHendelseType {
    GRUNNLAGSVERSJONERING_EVENT_NAME,
    FEILA,
    FORDELER_STATISTIKK, // FORDELER:STATISTIKK TODO: fiks denne
    ;

    override fun lagEventnameForType(): String = this.name
}
