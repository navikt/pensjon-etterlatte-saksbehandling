package no.nav.etterlatte.libs.common.vedtak

enum class VedtakKafkaHendelseType {
    VILKAARSVURDERT,
    BEREGNET,
    FATTET,
    ATTESTERT,
    UNDERKJENT,
    IVERKSATT,
    ;

    override fun toString(): String {
        return "VEDTAK:$name"
    }
}
