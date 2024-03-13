package no.nav.etterlatte.rapidsandrivers

import no.nav.etterlatte.libs.common.event.EventnameHendelseType

enum class ReguleringHendelseType : EventnameHendelseType {
    FINN_LOEPENDE_YTELSER,
    LOEPENDE_YTELSE_FUNNET,
    BEHANDLING_OPPRETTA,
    VILKAARSVURDERT,
    BEREGNA,
    START_REGULERING,
    ;

    override fun lagEventnameForType(): String = "REGULERING:${this.name}"
}
