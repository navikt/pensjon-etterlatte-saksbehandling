package no.nav.etterlatte.rapidsandrivers.migrering

import no.nav.etterlatte.libs.common.event.EventnameHendelseType

enum class Migreringshendelser : EventnameHendelseType {
    BEREGNET_FERDIG,
    IVERKSATT,
    AVBRYT_BEHANDLING,
    FIKS_ENKELTBREV,
    ALLEREDE_GJENOPPRETTA,
    ;

    override fun lagEventnameForType(): String = "MIGRERING:${this.name}"
}
