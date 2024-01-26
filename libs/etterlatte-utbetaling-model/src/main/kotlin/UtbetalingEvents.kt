package no.nav.etterlatte.utbetaling.common

import no.nav.etterlatte.libs.common.event.EventnameHendelseType

const val UTBETALING_RESPONSE = "utbetaling_response"

enum class UtbetalinghendelseType : EventnameHendelseType {
    EVENT_NAME_UTBETALING_OPPDATERT,
    ;

    override fun lagEventnameForType(): String = "UTBETALING:${this.name}"
}
