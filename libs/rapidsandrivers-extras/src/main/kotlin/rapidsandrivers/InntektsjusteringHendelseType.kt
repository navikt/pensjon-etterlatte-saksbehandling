package no.nav.etterlatte.rapidsandrivers

import no.nav.etterlatte.libs.common.event.EventnameHendelseType

enum class InntektsjusteringHendelseType : EventnameHendelseType {
    SEND_INFOBREV,
    ;

    override fun lagEventnameForType(): String = "INNTEKTSJUSTERING:${this.name}"
}
