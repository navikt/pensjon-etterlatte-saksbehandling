package no.nav.etterlatte.rapidsandrivers

import no.nav.etterlatte.libs.common.event.EventnameHendelseType

enum class EventNames(
    val eventname: String,
) : EventnameHendelseType {
    FEILA("FEILA"),
    FORDELER_STATISTIKK("FORDELER:STATISTIKK"),
    ALDERSOVERGANG("ALDERSOVERGANG"),
    NY_OPPLYSNING("OPPLYSNING:NY"),
    ;

    override fun lagEventnameForType(): String = this.eventname
}
