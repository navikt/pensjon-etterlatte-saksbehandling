package no.nav.etterlatte.behandling

import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.ManueltOpphoerAarsak
import no.nav.etterlatte.libs.common.behandling.OppgaveStatus
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import java.lang.RuntimeException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

sealed interface Behandling {
    fun toDetaljertBehandling(): DetaljertBehandling {
        val (soeknadMottatDato, gyldighetsproeving) = when (this) {
            is Foerstegangsbehandling -> this.soeknadMottattDato to this.gyldighetsproeving
            // TODO øh 24.10.2022 er det riktig at søknadMottatDato er behandlingOpprettet der vi ikke har søknad?
            else -> this.behandlingOpprettet to null
        }

        return DetaljertBehandling(
            id = id,
            sak = sak,
            behandlingOpprettet = behandlingOpprettet,
            sistEndret = sistEndret,
            soeknadMottattDato = soeknadMottatDato,
            innsender = persongalleri.innsender,
            soeker = persongalleri.soeker,
            gjenlevende = persongalleri.gjenlevende,
            avdoed = persongalleri.avdoed,
            soesken = persongalleri.soesken,
            gyldighetsproeving = gyldighetsproeving,
            status = status,
            behandlingType = type
        )
    }

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
    override val persongalleri: Persongalleri,
    private var virkningstidspunkt: Virkningstidspunkt?,
    val soeknadMottattDato: LocalDateTime,
    val gyldighetsproeving: GyldighetsResultat?
) : Behandling {
    fun hentVirkningstidspunkt() = virkningstidspunkt
    fun oppdaterVirkningstidspunkt(dato: LocalDate, kilde: Grunnlagsopplysning.Saksbehandler) {
        if (BehandlingStatus.underBehandling().contains(this.status)) {
            virkningstidspunkt = Virkningstidspunkt(YearMonth.from(dato), kilde)
        } else {
            throw RuntimeException("Kan ikke endre virkningstidspunkt for behandling som ikke er under behandling.")
        }
    }
}

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