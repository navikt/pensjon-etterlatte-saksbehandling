package no.nav.etterlatte.libs.common.behandling

enum class BehandlingStatus {
    OPPRETTET,
    GYLDIG_SOEKNAD,
    IKKE_GYLDIG_SOEKNAD,
    UNDER_BEHANDLING,
    FATTET_VEDTAK,
    ATTESTERT,
    RETURNERT,
    IVERKSATT,
    AVBRUTT;

    fun kanAvbrytes(): Boolean {
        return this !in iverksattEllerAttestert() && this != AVBRUTT
    }

    companion object {
        fun underBehandling() = listOf(
            OPPRETTET,
            GYLDIG_SOEKNAD,
            UNDER_BEHANDLING,
            RETURNERT,
            FATTET_VEDTAK
        )

        fun iverksattEllerAttestert() = listOf(
            IVERKSATT,
            ATTESTERT
        )

        fun kanRedigeres() = underBehandling() - FATTET_VEDTAK

        fun ikkeAvbrutt() = iverksattEllerAttestert() + underBehandling()
    }
}