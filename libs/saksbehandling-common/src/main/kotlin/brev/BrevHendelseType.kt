package no.nav.etterlatte.libs.common.brev

import no.nav.etterlatte.libs.common.event.EventnameHendelseType

enum class BrevHendelseType : EventnameHendelseType {
    FIKS_ENKELTBREV, // TODO: fjern og valider at den brukes der den skal

    OPPRETTET,
    JOURNALFOERT,
    DISTRIBUERT,
    ;

    override fun lagEventnameForType() = "BREV:${this.name}"
}
