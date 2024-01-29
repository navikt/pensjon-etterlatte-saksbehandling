package no.nav.etterlatte.libs.common.vedtak

import no.nav.etterlatte.libs.common.event.EventnameHendelseType

enum class VedtakKafkaHendelseHendelseType : EventnameHendelseType {
    FATTET,
    ATTESTERT,
    UNDERKJENT,
    TIL_SAMORDNING,
    SAMORDNET,
    IVERKSATT,
    SAMORDNING_MOTTATT,
    ;

    override fun lagEventnameForType() = "VEDTAK:$name"
}
