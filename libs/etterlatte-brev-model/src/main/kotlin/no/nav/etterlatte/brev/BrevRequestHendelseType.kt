package no.nav.etterlatte.brev

import no.nav.etterlatte.libs.common.event.EventnameHendelseType

enum class BrevRequestHendelseType : EventnameHendelseType {
    OPPRETT_JOURNALFOER_OG_DISTRIBUER,
    OPPRETT_DISTRIBUER_VARSEL_OG_VEDTAK,
    ;

    override fun lagEventnameForType(): String = "BREV:${this.name}"
}

const val BREVMAL_RIVER_KEY = "brevmal"
