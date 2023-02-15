package no.nav.etterlatte.oppgave.domain

import no.nav.etterlatte.libs.common.behandling.BehandlingType

enum class OppgaveType {

    FØRSTEGANGSBEHANDLING, REVURDERING, REGULERING, MANUELT_OPPHOER, ENDRING_PAA_SAK;

    companion object {
        fun fraBehandlingsType(behandlingType: BehandlingType): OppgaveType = when (behandlingType) {
            BehandlingType.FØRSTEGANGSBEHANDLING -> FØRSTEGANGSBEHANDLING
            BehandlingType.REVURDERING -> REVURDERING
            BehandlingType.REGULERING -> REGULERING
            BehandlingType.MANUELT_OPPHOER -> MANUELT_OPPHOER
        }
    }
}