package no.nav.etterlatte.libs.common.vedtak

enum class KafkaHendelseType {
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
