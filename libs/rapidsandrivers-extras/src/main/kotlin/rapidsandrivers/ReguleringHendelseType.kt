package no.nav.etterlatte.rapidsandrivers

import no.nav.etterlatte.libs.common.event.EventnameHendelseType

enum class OmregningHendelseType : EventnameHendelseType {
    KLAR_FOR_OMREGNING,
    BEHANDLING_OPPRETTA,
    VILKAARSVURDERT,
    TRYGDETID_KOPIERT,
    BEREGNA,
    ;

    override fun lagEventnameForType(): String = "OMREGNING:${this.name}"
}

enum class ReguleringHendelseType : EventnameHendelseType {
    FINN_SAKER_TIL_REGULERING,
    REGULERING_STARTA,
    SAK_FUNNET,
    LOEPENDE_YTELSE_FUNNET,
    YTELSE_IKKE_LOEPENDE,
    ;

    override fun lagEventnameForType(): String = "REGULERING:${this.name}"
}
