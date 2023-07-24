package no.nav.etterlatte.oppgaveny

import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.sak.SakDao
import no.nav.etterlatte.tilgangsstyring.SaksbehandlerMedRoller
import org.slf4j.LoggerFactory
import java.util.*

class OppgaveServiceNy(private val oppgaveDaoNy: OppgaveDaoNy, private val sakDao: SakDao) {

    private val logger = LoggerFactory.getLogger(OppgaveServiceNy::class.java)

    fun migrerSaker() {
        val oppgaver = inTransaction {
            oppgaveDaoNy.hentOppgaver()
        }
        val sakerTilMigrering = oppgaver.filter { oppgaveNy -> oppgaveNy.fnr === null || oppgaveNy.sakType === null }
        logger.info("Antall saker til migrering: ${sakerTilMigrering.size}")
        val alleSaker = sakDao.hentSaker()
        val sakMap = alleSaker.map { it.id to it }.toMap()

        val migrerteSaker: List<Triple<String, SakType?, UUID?>> = sakerTilMigrering.map {
            val sak = sakMap.get(it.sakId)
            if (sak != null) {
                Triple(sak.ident, sak.sakType, it.id)
            } else {
                logger.error("Fant ingen sak med id: ${it.sakId} for oppgave ${it.id}")
                Triple("", null, null)
            }
        }
        logger.info("antall saker til migrering ${migrerteSaker.size}")
        migrerteSaker.filter { it.second != null }
            .forEach {
                oppgaveDaoNy.migrerSak(it.first, it.second!!, it.third!!)
            }
    }

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