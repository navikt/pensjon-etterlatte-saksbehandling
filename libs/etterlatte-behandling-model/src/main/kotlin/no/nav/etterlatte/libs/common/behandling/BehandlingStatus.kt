package no.nav.etterlatte.libs.common.behandling

enum class BehandlingStatus {
    OPPRETTET,
    VILKAARSVURDERT,
    TRYGDETID_OPPDATERT,
    BEREGNET,
    AVKORTET,
    FATTET_VEDTAK,
    ATTESTERT,
    RETURNERT,
    TIL_SAMORDNING,
    SAMORDNET,
    IVERKSATT,
    AVBRUTT,
    ;

    fun kanAvbrytes(): Boolean {
        return this !in iverksattEllerAttestert() && this != AVBRUTT
    }

    fun kanEndres() = this in BehandlingStatus.kanEndres()

    companion object {
        fun underBehandling() =
            listOf(
                OPPRETTET,
                VILKAARSVURDERT,
                TRYGDETID_OPPDATERT,
                BEREGNET,
                AVKORTET,
                RETURNERT,
                FATTET_VEDTAK,
            )

        fun iverksattEllerAttestert() =
            listOf(
                IVERKSATT,
                SAMORDNET,
                TIL_SAMORDNING,
                ATTESTERT,
            )

        fun kanEndres() = underBehandling() - FATTET_VEDTAK

        fun ikkeAvbrutt() = iverksattEllerAttestert() + underBehandling()

        fun skalIkkeOmregnesVedGRegulering() =
            listOf(
                IVERKSATT,
                TIL_SAMORDNING,
                SAMORDNET,
                AVBRUTT,
                ATTESTERT,
                OPPRETTET,
                VILKAARSVURDERT,
                TRYGDETID_OPPDATERT,
            )
    }
}
