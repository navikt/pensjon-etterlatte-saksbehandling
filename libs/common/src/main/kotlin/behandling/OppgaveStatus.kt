package no.nav.etterlatte.libs.common.behandling

enum class OppgaveStatus {
    NY,
    TIL_ATTESTERING,
    RETURNERT,
    LUKKET;

    companion object {
        fun from(behandlingStatus: BehandlingStatus) {
            when (behandlingStatus) {
                BehandlingStatus.OPPRETTET,
                BehandlingStatus.VILKAARSVURDERT,
                BehandlingStatus.BEREGNET -> NY

                BehandlingStatus.FATTET_VEDTAK -> TIL_ATTESTERING
                BehandlingStatus.RETURNERT -> RETURNERT

                BehandlingStatus.AVBRUTT,
                BehandlingStatus.ATTESTERT,
                BehandlingStatus.IVERKSATT -> LUKKET
            }
        }
    }
}