package no.nav.etterlatte.libs.common.vedtak

import no.nav.etterlatte.libs.common.rapidsandrivers.EventnameHendelseType

enum class VedtakKafkaHendelseHendelseType : EventnameHendelseType {
    VILKAARSVURDERT, // UBRUKT
    BEREGNET, // UBRUKT
    FATTET,
    ATTESTERT,
    UNDERKJENT,
    TIL_SAMORDNING,
    SAMORDNET,
    IVERKSATT,
    ;

    override fun lagEventnameForType() = "VEDTAK:$name"
}
