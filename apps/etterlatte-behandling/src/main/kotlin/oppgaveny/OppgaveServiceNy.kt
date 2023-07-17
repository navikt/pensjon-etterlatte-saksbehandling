package no.nav.etterlatte.oppgaveny

import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.sak.SakDao
import no.nav.etterlatte.tilgangsstyring.SaksbehandlerMedRoller
import java.util.*

class OppgaveServiceNy(private val oppgaveDaoNy: OppgaveDaoNy, private val sakDao: SakDao) {
    // bruker: SaksbehandlerMedRoller må på en måte inn her
    fun finnOppgaverForBruker(bruker: SaksbehandlerMedRoller): List<OppgaveNy> {
        return inTransaction {
            oppgaveDaoNy.hentOppgaver()
        }
    }

    fun tildelSaksbehandler(saksbehandlerEndringDto: SaksbehandlerEndringDto) {
        inTransaction {
            val hentetOppgave = oppgaveDaoNy.hentOppgave(saksbehandlerEndringDto.oppgaveId)

            if (hentetOppgave != null) {
                if (hentetOppgave.saksbehandler.isNullOrEmpty()) {
                    oppgaveDaoNy.settNySaksbehandler(saksbehandlerEndringDto)
                } else {
                    throw BadRequestException("Oppgaven har allerede en saksbehandler")
                }
            } else {
                throw NotFoundException("Oppgaven finnes ikke")
            }
        }
    }

    fun byttSaksbehandler(saksbehandlerEndringDto: SaksbehandlerEndringDto) {
        inTransaction {
            val hentetOppgave = oppgaveDaoNy.hentOppgave(saksbehandlerEndringDto.oppgaveId)
            if (hentetOppgave != null) {
                oppgaveDaoNy.settNySaksbehandler(saksbehandlerEndringDto)
            } else {
                throw NotFoundException("Oppgaven finnes ikke")
            }
        }
    }

    fun fjernSaksbehandler(oppgaveId: OppgaveId) {
        inTransaction {
            val hentetOppgave = oppgaveDaoNy.hentOppgave(oppgaveId.oppgaveId)
            if (hentetOppgave != null) {
                if (hentetOppgave.saksbehandler != null) {
                    oppgaveDaoNy.fjernSaksbehandler(oppgaveId.oppgaveId)
                } else {
                    throw BadRequestException("Oppgaven har ingen saksbehandler")
                }
            } else {
                throw NotFoundException("Oppgaven finnes ikke")
            }
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

    private fun lagreOppgave(oppgaveNy: OppgaveNy): OppgaveNy {
        oppgaveDaoNy.lagreOppgave(oppgaveNy)
        return oppgaveDaoNy.hentOppgave(oppgaveNy.id)!!
    }

    fun hentOppgve(oppgaveId: UUID): OppgaveNy? {
        return oppgaveDaoNy.hentOppgave(oppgaveId)
    }
}