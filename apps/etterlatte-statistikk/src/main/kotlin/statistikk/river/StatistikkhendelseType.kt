package no.nav.etterlatte.statistikk.river

import no.nav.etterlatte.libs.common.event.EventnameHendelseType

enum class StatistikkhendelseType : EventnameHendelseType {
    REGISTRERT,
    ;

    override fun lagEventnameForType(): String = "STATISTIKK:${this.name}"
}
