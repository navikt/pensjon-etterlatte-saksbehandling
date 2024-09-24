package no.nav.etterlatte.rapidsandrivers

import no.nav.etterlatte.libs.common.event.EventnameHendelseType

/*
* En hendelseflyt som automatisk revurderer en sak.
* Flyt benyttes fra flere hold blant annet Regulering, Inntektsjsutering, etc.
*
* TODO omregning med vedtaksbrev støttes ikke enda
*/
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

/*
* TODO
* Ubrukt enn så lenge men tanken er at dette vil bli starten på en flyt
* til en jobb hvor flere saker gjennomfører omregning
*/
enum class MasseOmregningHendelseType : EventnameHendelseType {
    START_MASSE_OMREGNING,
    ;

    override fun lagEventnameForType(): String = "MASSE_OMREGNING:${this.name}"
}
