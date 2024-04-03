package no.nav.etterlatte.rapidsandrivers.migrering

import no.nav.etterlatte.libs.common.event.EventnameHendelseType

enum class Ventehendelser : EventnameHendelseType {
    TA_AV_VENT,
    TATT_AV_VENT,
    TATT_AV_VENT_UNDER_20_SJEKKA,
    TATT_AV_VENT_FYLT_20,
    ;

    override fun lagEventnameForType() = "VENT:${this.name}"
}
