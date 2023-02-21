package no.nav.etterlatte.oppgave.domain

import no.nav.etterlatte.libs.common.behandling.BehandlingType

enum class OppgaveType {

    FØRSTEGANGSBEHANDLING, REVURDERING, OMREGNING, MANUELT_OPPHOER, ENDRING_PAA_SAK;

    companion object {
        fun fraBehandlingsType(behandlingType: BehandlingType): OppgaveType = when (behandlingType) {
            BehandlingType.FØRSTEGANGSBEHANDLING -> FØRSTEGANGSBEHANDLING
            BehandlingType.REVURDERING -> REVURDERING
            BehandlingType.OMREGNING -> OMREGNING
            BehandlingType.MANUELT_OPPHOER -> MANUELT_OPPHOER
        }
    }
}