package no.nav.etterlatte.rapidsandrivers.migrering

import no.nav.etterlatte.libs.common.event.EventnameHendelseType

enum class Ventehendelser : EventnameHendelseType {
    TA_AV_VENT,
    TATT_AV_VENT,
    TATT_AV_VENT_UNDER_20_SJEKKA,
    ;

    override fun lagEventnameForType() = "VENT:${this.name}"
}
