package no.nav.etterlatte.rapidsandrivers

import no.nav.etterlatte.libs.common.event.EventnameHendelseType

enum class ReguleringHendelseType : EventnameHendelseType {
    FINN_LOEPENDE_YTELSER,
    OMREGNINGSHENDELSE,
    VILKAARSVURDER,
    BEREGN,
    OPPRETT_VEDTAK,
    START_REGULERING,
    ;

    override fun lagEventnameForType(): String = "REGULERING:${this.name}"
}
