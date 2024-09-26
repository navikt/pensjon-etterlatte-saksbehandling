package no.nav.etterlatte.brev

import no.nav.etterlatte.libs.common.event.EventnameHendelseType

enum class BrevRequestHendelseType : EventnameHendelseType {
    OPPRETT_JOURNALFOER_OG_DISTRIBUER,
    ;

    override fun lagEventnameForType(): String = "BREV:${this.name}"
}

const val BREVMAL_RIVER_KEY = "brevmal"
