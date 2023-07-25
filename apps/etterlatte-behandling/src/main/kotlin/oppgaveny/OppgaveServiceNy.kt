package no.nav.etterlatte.oppgaveny

import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.oppgaveNy.AttesteringsOppgave
import no.nav.etterlatte.libs.common.oppgaveNy.FjernSaksbehandlerRequest
import no.nav.etterlatte.libs.common.oppgaveNy.OppgaveNy
import no.nav.etterlatte.libs.common.oppgaveNy.OppgaveType
import no.nav.etterlatte.libs.common.oppgaveNy.RedigerFristRequest
import no.nav.etterlatte.libs.common.oppgaveNy.SaksbehandlerEndringDto
import no.nav.etterlatte.libs.common.oppgaveNy.Status
import no.nav.etterlatte.libs.common.oppgaveNy.opprettNyOppgaveMedReferanseOgSak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
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
                    throw BadRequestException(
                        "Oppgaven har allerede en saksbehandler, id: ${saksbehandlerEndringDto.oppgaveId}"
                    )
                }
            } else {
                throw NotFoundException("Oppgaven finnes ikke, id: ${saksbehandlerEndringDto.oppgaveId}")
            }
        }
    }

    fun byttSaksbehandler(saksbehandlerEndringDto: SaksbehandlerEndringDto) {
        inTransaction {
            val hentetOppgave = oppgaveDaoNy.hentOppgave(saksbehandlerEndringDto.oppgaveId)
            if (hentetOppgave != null) {
                oppgaveDaoNy.settNySaksbehandler(saksbehandlerEndringDto)
            } else {
                throw NotFoundException("Oppgaven finnes ikke, id: ${saksbehandlerEndringDto.oppgaveId}")
            }
        }
    }

    fun fjernSaksbehandler(fjernSaksbehandlerRequest: FjernSaksbehandlerRequest) {
        inTransaction {
            val hentetOppgave = oppgaveDaoNy.hentOppgave(fjernSaksbehandlerRequest.oppgaveId)
            if (hentetOppgave != null) {
                if (hentetOppgave.saksbehandler != null) {
                    oppgaveDaoNy.fjernSaksbehandler(fjernSaksbehandlerRequest.oppgaveId)
                } else {
                    throw BadRequestException(
                        "Oppgaven har ingen saksbehandler, id: ${fjernSaksbehandlerRequest.oppgaveId}"
                    )
                }
            } else {
                throw NotFoundException("Oppgaven finnes ikke, id: ${fjernSaksbehandlerRequest.oppgaveId}")
            }
        }
    }

    fun redigerFrist(redigerFristRequest: RedigerFristRequest) {
        inTransaction {
            val hentetOppgave = oppgaveDaoNy.hentOppgave(redigerFristRequest.oppgaveId)
            if (redigerFristRequest.frist.isBefore(Tidspunkt.now())) {
                throw BadRequestException("Tidspunkt tilbake i tid id: ${redigerFristRequest.oppgaveId}")
            }
            if (hentetOppgave != null) {
                if (hentetOppgave.saksbehandler != null) {
                    oppgaveDaoNy.redigerFrist(redigerFristRequest)
                } else {
                    throw BadRequestException(
                        "Oppgaven har ingen saksbehandler, id: ${redigerFristRequest.oppgaveId}"
                    )
                }
            } else {
                throw NotFoundException("Oppgaven finnes ikke, id: ${redigerFristRequest.oppgaveId}")
            }
        }
    }

    fun haandterFattetvedtak(attesteringsoppgave: AttesteringsOppgave): OppgaveNy {
        val behandlingsoppgaver = oppgaveDaoNy.hentOppgaverForBehandling(attesteringsoppgave.referanse)
        if (behandlingsoppgaver.isEmpty()) {
            throw BadRequestException("Må ha en oppgave for å kunne lage attesteringsoppgave")
        } else {
            val oppgaverUnderbehandling = behandlingsoppgaver.filter { it.status == Status.UNDER_BEHANDLING }
            if (oppgaverUnderbehandling.size > 1) {
                throw BadRequestException(
                    "Skal kun ha en oppgave under behandling, gjelder behandling:" +
                        " ${attesteringsoppgave.referanse}"
                )
            } else if (oppgaverUnderbehandling.isEmpty()) {
                throw BadRequestException(
                    "Det må finnes en oppgave under behandling, gjelder behandling:" +
                        " ${attesteringsoppgave.referanse}"
                )
            } else {
                val oppgaveUnderbehandling = oppgaverUnderbehandling[0]
                oppgaveDaoNy.endreStatusPaaOppgave(oppgaveUnderbehandling.id, Status.FERDIGSTILT)
                return opprettNyOppgaveMedSakOgReferanse(
                    attesteringsoppgave.referanse,
                    attesteringsoppgave.sakId,
                    attesteringsoppgave.oppgaveType
                )
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
        var oppgaveLagres = oppgaveNy
        if (oppgaveNy.frist === null) {
            val enMaanedFrem = oppgaveNy.opprettet.toLocalDatetimeUTC().plusMonths(1L).toTidspunkt()
            oppgaveLagres = oppgaveNy.copy(frist = enMaanedFrem)
        }
        oppgaveDaoNy.lagreOppgave(oppgaveLagres)
        return oppgaveDaoNy.hentOppgave(oppgaveLagres.id)!!
    }
    fun hentOppgave(oppgaveId: UUID): OppgaveNy? {
        return oppgaveDaoNy.hentOppgave(oppgaveId)
    }
}