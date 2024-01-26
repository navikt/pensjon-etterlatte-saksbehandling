package no.nav.etterlatte.rivers

import no.nav.etterlatte.libs.common.rapidsandrivers.EventnameHendelseType

enum class BrevHendelseHendelseType : EventnameHendelseType {
    FIKS_ENKELTBREV,
    OPPRETTET,
    JOURNALFOERT,
    DISTRIBUERT,
    ;

    override fun lagEventnameForType() = "BREV:${this.name}"
}
