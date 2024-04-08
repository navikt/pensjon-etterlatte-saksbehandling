package no.nav.etterlatte.rapidsandrivers

import no.nav.etterlatte.libs.common.event.EventnameHendelseType

enum class ReguleringHendelseType : EventnameHendelseType {
    START_REGULERING,
    FINN_LOEPENDE_YTELSER,
    LOEPENDE_YTELSE_FUNNET,
    BEHANDLING_OPPRETTA,
    VILKAARSVURDERT,
    BEREGNA,
    ;

    override fun lagEventnameForType(): String = "REGULERING:${this.name}"
}
