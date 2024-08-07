package no.nav.etterlatte.rapidsandrivers.migrering

import no.nav.etterlatte.libs.common.event.EventnameHendelseType

enum class Migreringshendelser : EventnameHendelseType {
    AVBRYT_BEHANDLING,
    FIKS_ENKELTBREV,
    ;

    override fun lagEventnameForType(): String = "MIGRERING:${this.name}"
}
