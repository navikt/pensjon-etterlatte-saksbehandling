package no.nav.etterlatte.rapidsandrivers.migrering

import no.nav.etterlatte.libs.common.event.EventnameHendelseType

enum class Migreringshendelser : EventnameHendelseType {
    MIGRER_SPESIFIKK_SAK,
    MIGRER_SAK,
    VERIFISER,
    LAGRE_KOPLING,
    LAGRE_GRUNNLAG,
    VILKAARSVURDER,
    BEREGN,
    TRYGDETID,
    BEREGNET_FERDIG,
    PAUSE,
    IVERKSATT,
    AVBRYT_BEHANDLING,
    FIKS_ENKELTBREV,
    ALLEREDE_GJENOPPRETTA,
    ;

    override fun lagEventnameForType(): String = "MIGRERING:${this.name}"
}
