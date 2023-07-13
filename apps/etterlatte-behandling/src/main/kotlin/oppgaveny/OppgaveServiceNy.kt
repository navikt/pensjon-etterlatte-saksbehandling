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

    fun tildelSaksbehandler(nySaksbehandlerDto: NySaksbehandlerDto) {
        val hentetOppgave = oppgaveDaoNy.hentOppgave(nySaksbehandlerDto.oppgaveId)
        if (hentetOppgave != null) {
            if (hentetOppgave.saksbehandler.isNullOrEmpty()) {
                oppgaveDaoNy.settNySaksbehandler(nySaksbehandlerDto)
            } else {
                throw RuntimeException("Oppgaven har allerede en saksbehandler")
            }
        } else {
            throw RuntimeException("Oppgave finnes ikke")
        }
    }

    fun byttSaksbehandler(nySaksbehandlerDto: NySaksbehandlerDto) {
        val hentetOppgave = oppgaveDaoNy.hentOppgave(nySaksbehandlerDto.oppgaveId)
        if (hentetOppgave != null) {
            oppgaveDaoNy.settNySaksbehandler(nySaksbehandlerDto)
        } else {
            throw RuntimeException("Oppgave finnes ikke")
        }
    }

    fun opprettNyOppgaveMedSakOgReferanse(referanse: String, sakId: Long, oppgaveType: OppgaveType): OppgaveNy {
        val sak = sakDao.hentSak(sakId)!!
        return lagreOppgave(
            opprettNyOppgaveMedReferanseOgSak(
                referanse = referanse,
                sak = sak,
                oppgaveType = oppgaveType
            )
        )
    }

    fun lagreOppgave(oppgaveNy: OppgaveNy): OppgaveNy {
        oppgaveDaoNy.lagreOppgave(oppgaveNy)
        return oppgaveDaoNy.hentOppgave(oppgaveNy.id)!!
    }
}