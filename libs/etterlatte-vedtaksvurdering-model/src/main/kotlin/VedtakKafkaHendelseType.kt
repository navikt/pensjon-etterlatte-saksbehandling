package no.nav.etterlatte.libs.common.vedtak

import no.nav.etterlatte.event.EventName

enum class VedtakKafkaHendelseType : EventName {
    VILKAARSVURDERT,
    BEREGNET,
    FATTET,
    ATTESTERT,
    UNDERKJENT,
    TIL_SAMORDNING,
    SAMORDNET,
    IVERKSATT,
    ;

    override fun toString(): String {
        return "VEDTAK:$name"
    }

    override fun toEventName() = toString()
}
