package no.nav.etterlatte.libs.common.vedtak

import no.nav.etterlatte.libs.common.event.EventnameHendelseType

enum class VedtakAldersovergangEvents : EventnameHendelseType {
    SJEKK_LOEPENDE_YTELSE,
    SJEKK_LOEPENDE_YTELSE_RESULTAT,
    ;

    override fun lagEventnameForType(): String = "ALDERSOVERGANG:VEDTAK:${this.name}"
}
