package no.nav.etterlatte.sak

import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.tilgangsstyring.SaksbehandlerMedRoller

interface TilgangService {
    fun harTilgangTilBehandling(behandlingId: String, saksbehandlerMedRoller: SaksbehandlerMedRoller): Boolean
    fun oppdaterAdressebeskyttelse(id: Long, adressebeskyttelseGradering: AdressebeskyttelseGradering): Int
    fun harTilgangTilSak(sakId: Long, saksbehandlerMedRoller: SaksbehandlerMedRoller): Boolean
    fun harTilgangTilPerson(fnr: String, saksbehandlerMedRoller: SaksbehandlerMedRoller): Boolean
    fun harTilgangTilOppgave(oppgaveId: String, saksbehandlerMedRoller: SaksbehandlerMedRoller): Boolean
}

data class SakMedGraderingOgSkjermet(
    val id: Long,
    val adressebeskyttelseGradering: AdressebeskyttelseGradering?,
    val erSkjermet: Boolean?
)

data class SakMedGradering(
    val id: Long,
    val adressebeskyttelseGradering: AdressebeskyttelseGradering?
)

class TilgangServiceImpl(
    private val dao: SakTilgangDao
) : TilgangService {

    override fun harTilgangTilPerson(fnr: String, saksbehandlerMedRoller: SaksbehandlerMedRoller): Boolean {
        val finnSakerMedGradering = dao.finnSakerMedGraderingOgSkjerming(fnr)
        if (finnSakerMedGradering.isEmpty()) {
            return true
        }
        return finnSakerMedGradering.map {
            harTilgangSjekker(it, saksbehandlerMedRoller)
        }.all { it }
    }

    override fun harTilgangTilOppgave(oppgaveId: String, saksbehandlerMedRoller: SaksbehandlerMedRoller): Boolean {
        val sakMedGraderingOgSkjermet = dao.hentSakMedGraderingOgSkjermingPaaOppgave(oppgaveId) ?: return true
        return harTilgangSjekker(sakMedGraderingOgSkjermet, saksbehandlerMedRoller)
    }

    override fun harTilgangTilSak(sakId: Long, saksbehandlerMedRoller: SaksbehandlerMedRoller): Boolean {
        val sak = dao.hentSakMedGraderingOgSkjerming(sakId) ?: return true
        return harTilgangSjekker(sak, saksbehandlerMedRoller)
    }

    override fun harTilgangTilBehandling(
        behandlingId: String,
        saksbehandlerMedRoller: SaksbehandlerMedRoller
    ): Boolean {
        val sakMedGraderingOgSkjermet =
            dao.hentSakMedGarderingOgSkjermingPaaBehandling(behandlingId) ?: return true
        return harTilgangSjekker(sakMedGraderingOgSkjermet, saksbehandlerMedRoller)
    }

    private fun harTilgangSjekker(
        sak: SakMedGraderingOgSkjermet,
        saksbehandlerMedRoller: SaksbehandlerMedRoller
    ): Boolean {
        return kanBehandleEgenAnsatt(sak, saksbehandlerMedRoller) &&
            kanBehandleAdressebeskyttelse(sak, saksbehandlerMedRoller)
    }

    private fun kanBehandleEgenAnsatt(
        sak: SakMedGraderingOgSkjermet,
        saksbehandlerMedRoller: SaksbehandlerMedRoller
    ): Boolean {
        return when (sak.erSkjermet) {
            true -> saksbehandlerMedRoller.harRolleEgenAnsatt()
            false -> true
            null -> true
        }
    }

    private fun kanBehandleAdressebeskyttelse(
        sak: SakMedGraderingOgSkjermet,
        saksbehandlerMedRoller: SaksbehandlerMedRoller
    ): Boolean {
        return when (sak.adressebeskyttelseGradering) {
            AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND -> saksbehandlerMedRoller.harRolleStrengtFortrolig()
            AdressebeskyttelseGradering.STRENGT_FORTROLIG -> saksbehandlerMedRoller.harRolleStrengtFortrolig()
            AdressebeskyttelseGradering.FORTROLIG -> saksbehandlerMedRoller.harRolleFortrolig()
            AdressebeskyttelseGradering.UGRADERT -> true
            else -> true
        }
    }

    override fun oppdaterAdressebeskyttelse(id: Long, adressebeskyttelseGradering: AdressebeskyttelseGradering): Int {
        return dao.oppdaterAdresseBeskyttelse(id, adressebeskyttelseGradering)
    }
}