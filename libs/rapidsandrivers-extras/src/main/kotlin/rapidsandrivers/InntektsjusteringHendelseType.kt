package no.nav.etterlatte.rapidsandrivers

import no.nav.etterlatte.libs.common.event.EventnameHendelseType

enum class InntektsjusteringHendelseType : EventnameHendelseType {
    START_INNTEKTSJUSTERING_JOBB,
    ;

    override fun lagEventnameForType(): String = "INNTEKTSJUSTERING:${this.name}"
}
