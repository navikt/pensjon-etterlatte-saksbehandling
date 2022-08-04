package no.nav.etterlatte

import no.nav.etterlatte.behandling.Foerstegangsbehandling
import no.nav.etterlatte.behandling.Revurdering
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.OppgaveStatus
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import java.time.LocalDateTime
import java.util.*

fun foerstegangsbehandling(
    id: UUID = UUID.randomUUID(),
    sak: Long,
    behandlingOpprettet: LocalDateTime = LocalDateTime.now(),
    sistEndret: LocalDateTime = LocalDateTime.now(),
    status: BehandlingStatus = BehandlingStatus.OPPRETTET,
    oppgaveStatus: OppgaveStatus = OppgaveStatus.NY,
    soeknadMottattDato: LocalDateTime = LocalDateTime.now(),
    persongalleri: Persongalleri = persongalleri(),
    gyldighetsproeving: GyldighetsResultat? = null
) = Foerstegangsbehandling(
    id = id,
    sak = sak,
    behandlingOpprettet = behandlingOpprettet,
    sistEndret = sistEndret,
    status = status,
    oppgaveStatus = oppgaveStatus,
    soeknadMottattDato = soeknadMottattDato,
    persongalleri = persongalleri,
    gyldighetsproeving = gyldighetsproeving,
)

fun revurdering(
    id: UUID = UUID.randomUUID(),
    sak: Long,
    behandlingOpprettet: LocalDateTime = LocalDateTime.now(),
    sistEndret: LocalDateTime = LocalDateTime.now(),
    status: BehandlingStatus = BehandlingStatus.OPPRETTET,
    oppgaveStatus: OppgaveStatus = OppgaveStatus.NY,
    persongalleri: Persongalleri = persongalleri(),
    revurderingAarsak: RevurderingAarsak
) = Revurdering(
    id = id,
    sak = sak,
    behandlingOpprettet = behandlingOpprettet,
    sistEndret = sistEndret,
    status = status,
    oppgaveStatus = oppgaveStatus,
    persongalleri = persongalleri,
    revurderingsaarsak = revurderingAarsak
)

fun persongalleri(
    soeker: String = "Soeker",
    innsender: String = "Innsender",
    soesken: List<String> = listOf("Soester", "Bror"),
    avdoed: List<String> = listOf("Avdoed"),
    gjenlevende: List<String> = listOf("Gjenlevende")
) = Persongalleri(
    soeker = soeker,
    innsender = innsender,
    soesken = soesken,
    avdoed = avdoed,
    gjenlevende = gjenlevende,
)
