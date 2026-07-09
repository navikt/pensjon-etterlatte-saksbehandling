package no.nav.etterlatte.rapidsandrivers

import no.nav.etterlatte.libs.common.event.EventnameHendelseType

enum class ReguleringHendelseType : EventnameHendelseType {
    REGULERING_STARTA,
    SAK_FUNNET,
    LOEPENDE_YTELSE_FUNNET,
    YTELSE_IKKE_LOEPENDE,
    ;

    override fun lagEventnameForType(): String = "REGULERING:${this.name}"
}
