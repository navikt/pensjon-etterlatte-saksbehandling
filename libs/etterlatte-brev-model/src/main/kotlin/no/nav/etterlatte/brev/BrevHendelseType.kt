package no.nav.etterlatte.brev

import no.nav.etterlatte.libs.common.event.EventnameHendelseType

enum class BrevHendelseType : EventnameHendelseType {
    OPPRETTET,
    JOURNALFOERT,
    DISTRIBUERT,
    ;

    override fun lagEventnameForType() = "BREV:${this.name}"
}
