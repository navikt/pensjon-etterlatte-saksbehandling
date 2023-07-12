package no.nav.etterlatte.oppgaveny

import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.sak.SakDao
import no.nav.etterlatte.tilgangsstyring.SaksbehandlerMedRoller

class OppgaveServiceNy(private val oppgaveDaoNy: OppgaveDaoNy, private val sakDao: SakDao) {
    // bruker: SaksbehandlerMedRoller må på en måte inn her
    fun finnOppgaverForBruker(bruker: SaksbehandlerMedRoller): List<OppgaveNy> {
        return inTransaction {
            oppgaveDaoNy.hentOppgaver()
        }
    }

    fun opprettNyOppgaveMedSakOgReferanse(referanse: String, sakId: Long, oppgaveType: OppgaveType) {
        val sak = sakDao.hentSak(sakId)!!
        lagreOppgave(opprettNyOppgaveMedReferanseOgSak(referanse = referanse, sak = sak, oppgaveType = oppgaveType))
    }

    fun lagreOppgave(oppgaveNy: OppgaveNy) { // TODO: skal det være noen verifisering her? evt en egen metode for route
        return oppgaveDaoNy.lagreOppgave(oppgaveNy)
    }
}