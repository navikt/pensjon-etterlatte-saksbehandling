package no.nav.etterlatte.rapidsandrivers.migrering

import no.nav.etterlatte.libs.common.rapidsandrivers.EventnameHendelseType

enum class Migreringshendelser : EventnameHendelseType {
    START_MIGRERING,
    MIGRER_SPESIFIKK_SAK,
    MIGRER_SAK,
    VERIFISER,
    LAGRE_KOPLING,
    LAGRE_GRUNNLAG,
    VILKAARSVURDER,
    BEREGN,
    TRYGDETID,
    VEDTAK,
    PAUSE,
    IVERKSATT,
    BREV_SENDT,
    AVBRYT_BEHANDLING,
    FIKS_ENKELTBREV,
    ;

    override fun lagEventnameForType(): String = "MIGRERING:${this.name}"
}
