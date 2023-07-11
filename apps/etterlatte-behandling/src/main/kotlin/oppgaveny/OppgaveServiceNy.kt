package no.nav.etterlatte.oppgaveny

import no.nav.etterlatte.tilgangsstyring.SaksbehandlerMedRoller

class OppgaveServiceNy(private val oppgaveDaoNy: OppgaveDaoNy) {
    fun finnOppgaverForBruker(bruker: SaksbehandlerMedRoller): List<OppgaveNy> {
        return oppgaveDaoNy.hentOppgaver()
    }

    fun lagreOppgave(oppgaveNy: OppgaveNy) {
        oppgaveDaoNy.lagreOppgave(oppgaveNy)
    }
}