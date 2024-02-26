package no.nav.etterlatte.libs.common.oppgave

import java.time.LocalDate
import java.util.UUID

data class VentefristGaarUtRequest(
    val dato: LocalDate,
    val type: OppgaveType,
    val oppgaveKilde: OppgaveKilde,
    val oppgaver: List<UUID>,
)
