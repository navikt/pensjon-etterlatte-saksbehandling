package no.nav.etterlatte.libs.common.oppgave

import no.nav.etterlatte.libs.common.sak.SakId
import java.time.LocalDate
import java.util.UUID

data class VentefristGaarUtRequest(
    val dato: LocalDate,
    val type: Collection<OppgaveType>,
    val oppgaveKilde: Collection<OppgaveKilde>,
    val oppgaver: List<UUID>,
    val grense: Int = 300,
)

data class VentefristerGaarUtResponse(
    val behandlinger: List<VentefristGaarUt>,
)

data class VentefristGaarUt(
    val sakId: SakId,
    val referanse: String,
    val oppgaveId: UUID,
    val oppgavekilde: OppgaveKilde,
    val merknad: String?,
)
