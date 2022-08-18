package no.nav.etterlatte.typer

import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.OppgaveStatus
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*

data class BehandlingsOppgave(
    val behandlingId: UUID,
    val behandlingStatus: BehandlingStatus,
    val oppgaveStatus: OppgaveStatus,
    val sak: Sak,
    val regdato: ZonedDateTime,
    val fristDato: LocalDate
)

data class OppgaveListe(val oppgaver: List<BehandlingsOppgave>)