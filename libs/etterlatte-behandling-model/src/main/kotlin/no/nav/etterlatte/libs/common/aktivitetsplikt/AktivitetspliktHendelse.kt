package no.nav.etterlatte.libs.common.aktivitetsplikt

import no.nav.etterlatte.libs.common.event.EventnameHendelseType

enum class AktivitetspliktHendelse : EventnameHendelseType {
    OPPDATERT,
    ;

    override fun lagEventnameForType(): String = "AKTIVITETSPLIKT:${this.name}"
}

const val AKTIVITETSPLIKT_DTO_RIVER_KEY = "aktivitetsplikt"
