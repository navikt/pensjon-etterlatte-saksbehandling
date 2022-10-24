package no.nav.etterlatte.behandling

import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.ManueltOpphoerAarsak
import no.nav.etterlatte.libs.common.behandling.OppgaveStatus
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import java.time.LocalDateTime
import java.util.*

sealed interface Behandling {
    fun toDetaljertBehandling(): DetaljertBehandling = DetaljertBehandling(
        id = id,
        sak = sak,
        behandlingOpprettet = behandlingOpprettet,
        sistEndret = sistEndret,
        soeknadMottattDato = (this as? Foerstegangsbehandling)?.soeknadMottattDato,
        innsender = persongalleri.innsender,
        soeker = persongalleri.soeker,
        gjenlevende = persongalleri.gjenlevende,
        avdoed = persongalleri.avdoed,
        soesken = persongalleri.soesken,
        gyldighetsproeving = (this as? Foerstegangsbehandling)?.gyldighetsproeving,
        status = status,
        behandlingType = type
    )

    val id: UUID
    val sak: Long
    val behandlingOpprettet: LocalDateTime
    val sistEndret: LocalDateTime
    val status: BehandlingStatus
    val oppgaveStatus: OppgaveStatus?
    val type: BehandlingType
    val persongalleri: Persongalleri
}

data class Foerstegangsbehandling(
    override val id: UUID,
    override val sak: Long,
    override val behandlingOpprettet: LocalDateTime,
    override val sistEndret: LocalDateTime,
    override val status: BehandlingStatus,
    override val oppgaveStatus: OppgaveStatus?,
    override val type: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
    val soeknadMottattDato: LocalDateTime,
    override val persongalleri: Persongalleri,
    val gyldighetsproeving: GyldighetsResultat?
) : Behandling

data class Revurdering(
    override val id: UUID,
    override val sak: Long,
    override val behandlingOpprettet: LocalDateTime,
    override val sistEndret: LocalDateTime,
    override val status: BehandlingStatus,
    override val oppgaveStatus: OppgaveStatus?,
    override val type: BehandlingType = BehandlingType.REVURDERING,
    override val persongalleri: Persongalleri,
    val revurderingsaarsak: RevurderingAarsak
) : Behandling

data class ManueltOpphoer(
    override val id: UUID,
    override val sak: Long,
    override val behandlingOpprettet: LocalDateTime,
    override val sistEndret: LocalDateTime,
    override val status: BehandlingStatus,
    override val oppgaveStatus: OppgaveStatus?,
    override val type: BehandlingType = BehandlingType.MANUELT_OPPHOER,
    override val persongalleri: Persongalleri,
    val opphoerAarsaker: List<ManueltOpphoerAarsak>,
    val fritekstAarsak: String?
) : Behandling {
    constructor(
        sak: Long,
        persongalleri: Persongalleri,
        opphoerAarsaker: List<ManueltOpphoerAarsak>,
        fritekstAarsak: String?
    ) : this(
        id = UUID.randomUUID(),
        sak = sak,
        behandlingOpprettet = LocalDateTime.now(),
        sistEndret = LocalDateTime.now(),
        status = BehandlingStatus.OPPRETTET,
        oppgaveStatus = OppgaveStatus.NY,
        type = BehandlingType.MANUELT_OPPHOER,
        persongalleri = persongalleri,
        opphoerAarsaker = opphoerAarsaker,
        fritekstAarsak = fritekstAarsak
    )
}