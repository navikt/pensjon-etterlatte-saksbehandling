package no.nav.etterlatte.behandling

import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.OppgaveStatus
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import java.time.LocalDateTime
import java.util.*

sealed interface Behandling {
    val id: UUID
    val sak: Long
    val behandlingOpprettet: LocalDateTime
    val sistEndret: LocalDateTime
    val status: BehandlingStatus
    val oppgaveStatus: OppgaveStatus?
    val type: BehandlingType
    val soeknadMottattDato: LocalDateTime // TODO: finne mer passende navn eller flytte til Foerstegangsbehandling - muligens mottattDato
}

data class Foerstegangsbehandling(
    override val id: UUID,
    override val sak: Long,
    override val behandlingOpprettet: LocalDateTime,
    override val sistEndret: LocalDateTime,
    override val status: BehandlingStatus,
    override val oppgaveStatus: OppgaveStatus?,
    override val type: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
    override val soeknadMottattDato: LocalDateTime,
    val persongalleri: Persongalleri,
    val gyldighetsproeving: GyldighetsResultat?,
) : Behandling

data class Revurdering(
    override val id: UUID,
    override val sak: Long,
    override val behandlingOpprettet: LocalDateTime,
    override val sistEndret: LocalDateTime,
    override val status: BehandlingStatus,
    override val oppgaveStatus: OppgaveStatus?,
    override val type: BehandlingType = BehandlingType.REVURDERING,
    override val soeknadMottattDato: LocalDateTime,
    val persongalleri: Persongalleri
) : Behandling
