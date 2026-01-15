package no.nav.etterlatte.libs.common.behandling

enum class BehandlingStatus {
    OPPRETTET,
    VILKAARSVURDERT,
    TRYGDETID_OPPDATERT,
    BEREGNET,
    AVKORTET,
    FATTET_VEDTAK,
    ATTESTERT,
    AVSLAG,
    RETURNERT,
    TIL_SAMORDNING,
    SAMORDNET,
    IVERKSATT,
    AVBRUTT,
    ATTESTERT_INGEN_ENDRING,
    ;

    fun kanAvbrytes(): Boolean = this !in iverksattEllerAttestert() && this != AVBRUTT

    fun aapenBehandling(): Boolean = this in underBehandling() + ATTESTERT

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
                AVSLAG,
                ATTESTERT_INGEN_ENDRING,
            )

        fun kanEndres() = underBehandling() - FATTET_VEDTAK

        fun skalIkkeOmregnesVedGRegulering() =
            listOf(
                IVERKSATT,
                TIL_SAMORDNING,
                SAMORDNET,
                AVBRUTT,
                ATTESTERT,
                OPPRETTET,
                VILKAARSVURDERT,
                AVSLAG,
                ATTESTERT_INGEN_ENDRING,
            )
    }
}
