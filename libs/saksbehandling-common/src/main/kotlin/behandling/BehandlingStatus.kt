package no.nav.etterlatte.libs.common.behandling

enum class BehandlingStatus {
    OPPRETTET,
    VILKAARSVURDERT,
    BEREGNET,
    AVKORTET,
    FATTET_VEDTAK,
    ATTESTERT,
    RETURNERT,
    IVERKSATT,
    AVBRUTT;

    fun kanAvbrytes(): Boolean {
        return this !in iverksattEllerAttestert() && this != AVBRUTT
    }

    fun kanEndres() = this in BehandlingStatus.kanEndres()

    companion object {
        fun underBehandling() = listOf(
            OPPRETTET,
            VILKAARSVURDERT,
            BEREGNET,
            AVKORTET,
            RETURNERT,
            FATTET_VEDTAK
        )

        fun iverksattEllerAttestert() = listOf(
            IVERKSATT,
            ATTESTERT
        )

        fun kanEndres() = underBehandling() - FATTET_VEDTAK

        fun ikkeAvbrutt() = iverksattEllerAttestert() + underBehandling()

        fun skalIkkeOmregnesVedGRegulering() = listOf(IVERKSATT, AVBRUTT, ATTESTERT, OPPRETTET, VILKAARSVURDERT)
    }
}