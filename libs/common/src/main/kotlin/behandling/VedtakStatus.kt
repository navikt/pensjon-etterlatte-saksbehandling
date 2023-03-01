package no.nav.etterlatte.libs.common.behandling

enum class VedtakStatus {
    BEREGNET, // TODO denne skal vekk men krever migrering i DB
    OPPRETTET,
    FATTET_VEDTAK,
    ATTESTERT,
    RETURNERT,
    IVERKSATT
}