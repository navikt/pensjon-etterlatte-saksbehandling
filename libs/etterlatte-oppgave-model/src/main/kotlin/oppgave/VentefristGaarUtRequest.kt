package no.nav.etterlatte.libs.common.oppgave

import java.time.LocalDate

data class VentefristGaarUtRequest(val dato: LocalDate, val type: OppgaveType, val oppgaveKilde: OppgaveKilde)
