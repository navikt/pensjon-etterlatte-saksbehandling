package no.nav.etterlatte.libs.common.event

import no.nav.etterlatte.libs.common.rapidsandrivers.EventnameHendelseType

const val BREVMAL_RIVER_KEY = "brevmal"

enum class BrevHendelseHendelseType : EventnameHendelseType {
    OPPRETT_BREV,
    OPPRETT_JOURNALFOER_OG_DISTRIBUER,
    ;

    override fun lagEventnameForType(): String = "BREV:${this.name}"
}
