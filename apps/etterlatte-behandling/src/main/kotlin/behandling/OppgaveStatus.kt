package no.nav.etterlatte.behandling

import no.nav.etterlatte.libs.common.behandling.BehandlingStatus

enum class OppgaveStatus {
    NY,
    TIL_ATTESTERING,
    RETURNERT,
    LUKKET;

    companion object {
        fun from(behandlingStatus: BehandlingStatus): OppgaveStatus {
            return when (behandlingStatus) {
                BehandlingStatus.OPPRETTET,
                BehandlingStatus.VILKAARSVURDERT,
                BehandlingStatus.BEREGNET,
                BehandlingStatus.AVKORTET -> NY

                BehandlingStatus.FATTET_VEDTAK -> TIL_ATTESTERING
                BehandlingStatus.RETURNERT -> RETURNERT

                BehandlingStatus.AVBRUTT,
                BehandlingStatus.ATTESTERT,
                BehandlingStatus.IVERKSATT -> LUKKET
            }
        }
    }
}