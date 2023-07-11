package no.nav.etterlatte.oppgaveny

import no.nav.etterlatte.sak.SakDao
import no.nav.etterlatte.tilgangsstyring.SaksbehandlerMedRoller

class OppgaveServiceNy(private val oppgaveDaoNy: OppgaveDaoNy, private val sakDao: SakDao) {
    // bruker: SaksbehandlerMedRoller må på en måte inn her
    fun finnOppgaverForBruker(bruker: SaksbehandlerMedRoller): List<OppgaveNy> {
        return oppgaveDaoNy.hentOppgaver()
    }

    fun opprettNyOppgaveMedSakOgReferanse(referanse: String, sakId: Long) {
        val sak = sakDao.hentSak(sakId)!!
        lagreOppgave(opprettNyOppgaveMedReferanseOgSak(referanse = referanse, sak = sak))
    }

    fun lagreOppgave(oppgaveNy: OppgaveNy) { // TODO: skal det være noen verifisering her? evt en egen metode for route
        oppgaveDaoNy.lagreOppgave(oppgaveNy)
    }
}