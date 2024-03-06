package no.nav.etterlatte.libs.common.oppgave

import java.time.LocalDate
import java.util.UUID

data class VentefristGaarUtRequest(
    val dato: LocalDate,
    val type: Set<OppgaveType>,
    val oppgaveKilde: Set<OppgaveKilde>,
    val oppgaver: List<UUID>,
)

data class VentefristerGaarUtResponse(
    val behandlinger: List<VentefristGaarUt>,
)

data class VentefristGaarUt(
    val sakId: Long,
    val behandlingId: UUID,
    val oppgaveID: UUID,
    val oppgavekilde: OppgaveKilde,
)
