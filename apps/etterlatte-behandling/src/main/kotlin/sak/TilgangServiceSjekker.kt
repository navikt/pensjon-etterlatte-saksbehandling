package no.nav.etterlatte.sak

import no.nav.etterlatte.libs.common.feilhaandtering.GenerellIkkeFunnetException
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.sak.SakMedGraderingOgSkjermet
import no.nav.etterlatte.tilgangsstyring.SaksbehandlerMedRoller
import java.util.UUID

interface TilgangServiceSjekker {
    fun harTilgangTilBehandling(
        behandlingId: UUID,
        saksbehandlerMedRoller: SaksbehandlerMedRoller,
    ): Boolean

    fun harTilgangTilSak(
        sakId: SakId,
        saksbehandlerMedRoller: SaksbehandlerMedRoller,
    ): Boolean

    fun harTilgangTilPerson(
        fnr: String,
        saksbehandlerMedRoller: SaksbehandlerMedRoller,
    ): Boolean

    fun harTilgangTilOppgave(
        oppgaveId: UUID,
        saksbehandlerMedRoller: SaksbehandlerMedRoller,
    ): Boolean

    fun harTilgangTilKlage(
        klageId: UUID,
        saksbehandlerMedRoller: SaksbehandlerMedRoller,
    ): Boolean

    fun harTilgangTilGenerellBehandling(
        behandlingId: UUID,
        saksbehandlerMedRoller: SaksbehandlerMedRoller,
    ): Boolean

    fun harTilgangTilTilbakekreving(
        tilbakekrevingId: UUID,
        saksbehandlerMedRoller: SaksbehandlerMedRoller,
    ): Boolean

    fun harTilgangTilEtteroppgjoer(
        etteroppgjoerId: UUID,
        saksbehandlerMedRoller: SaksbehandlerMedRoller,
    ): Boolean
}

class TilgangServiceSjekkerImpl(
    private val dao: SakTilgangDao,
) : TilgangServiceSjekker {
    override fun harTilgangTilPerson(
        fnr: String,
        saksbehandlerMedRoller: SaksbehandlerMedRoller,
    ): Boolean {
        val finnSakerMedGradering = dao.finnSakerMedGraderingOgSkjerming(fnr)
        if (finnSakerMedGradering.isEmpty()) {
            return true
        }
        return finnSakerMedGradering
            .map {
                harTilgangSjekker(it, saksbehandlerMedRoller)
            }.all { it }
    }

    override fun harTilgangTilOppgave(
        oppgaveId: UUID,
        saksbehandlerMedRoller: SaksbehandlerMedRoller,
    ): Boolean {
        val sakMedGraderingOgSkjermet =
            dao.hentSakMedGraderingOgSkjermingPaaOppgave(oppgaveId) ?: throw GenerellIkkeFunnetException()
        return harTilgangSjekker(sakMedGraderingOgSkjermet, saksbehandlerMedRoller)
    }

    override fun harTilgangTilKlage(
        klageId: UUID,
        saksbehandlerMedRoller: SaksbehandlerMedRoller,
    ): Boolean {
        val sakMedGraderingOgSkjermet =
            dao.hentSakMedGraderingOgSkjermingPaaKlage(klageId) ?: throw GenerellIkkeFunnetException()
        return harTilgangSjekker(sakMedGraderingOgSkjermet, saksbehandlerMedRoller)
    }

    override fun harTilgangTilGenerellBehandling(
        behandlingId: UUID,
        saksbehandlerMedRoller: SaksbehandlerMedRoller,
    ): Boolean {
        val sakMedGraderingOgSkjermet =
            dao.hentSakMedGraderingOgSkjermingPaaGenerellbehandling(behandlingId) ?: throw GenerellIkkeFunnetException()
        return harTilgangSjekker(sakMedGraderingOgSkjermet, saksbehandlerMedRoller)
    }

    override fun harTilgangTilTilbakekreving(
        tilbakekrevingId: UUID,
        saksbehandlerMedRoller: SaksbehandlerMedRoller,
    ): Boolean {
        val sakMedGraderingOgSkjermet =
            dao.hentSakMedGraderingOgSkjermingPaaTilbakekreving(tilbakekrevingId) ?: throw GenerellIkkeFunnetException()
        return harTilgangSjekker(sakMedGraderingOgSkjermet, saksbehandlerMedRoller)
    }

    override fun harTilgangTilEtteroppgjoer(
        etteroppgjoerId: UUID,
        saksbehandlerMedRoller: SaksbehandlerMedRoller,
    ): Boolean {
        val sakMedGraderingOgSkjermet =
            dao.hentSakMedGraderingOgSkjermingPaaEtteroppgjoer(etteroppgjoerId) ?: throw GenerellIkkeFunnetException()
        return harTilgangSjekker(sakMedGraderingOgSkjermet, saksbehandlerMedRoller)
    }

    override fun harTilgangTilSak(
        sakId: SakId,
        saksbehandlerMedRoller: SaksbehandlerMedRoller,
    ): Boolean {
        val sak = dao.hentSakMedGraderingOgSkjerming(sakId) ?: throw GenerellIkkeFunnetException()
        return harTilgangSjekker(sak, saksbehandlerMedRoller)
    }

    override fun harTilgangTilBehandling(
        behandlingId: UUID,
        saksbehandlerMedRoller: SaksbehandlerMedRoller,
    ): Boolean {
        val sakMedGraderingOgSkjermet =
            dao.hentSakMedGraderingOgSkjermingPaaBehandling(behandlingId) ?: throw GenerellIkkeFunnetException()
        return harTilgangSjekker(sakMedGraderingOgSkjermet, saksbehandlerMedRoller)
    }

    private fun harTilgangSjekker(
        sak: SakMedGraderingOgSkjermet,
        saksbehandlerMedRoller: SaksbehandlerMedRoller,
    ): Boolean =
        kanBehandleEgenAnsatt(sak, saksbehandlerMedRoller) &&
            kanBehandleAdressebeskyttelse(sak, saksbehandlerMedRoller)

    private fun kanBehandleEgenAnsatt(
        sak: SakMedGraderingOgSkjermet,
        saksbehandlerMedRoller: SaksbehandlerMedRoller,
    ): Boolean =
        when (sak.erSkjermet) {
            true -> saksbehandlerMedRoller.harRolleEgenAnsatt()
            false -> true
            null -> true
        }

    private fun kanBehandleAdressebeskyttelse(
        sak: SakMedGraderingOgSkjermet,
        saksbehandlerMedRoller: SaksbehandlerMedRoller,
    ): Boolean =
        when (sak.adressebeskyttelseGradering) {
            AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND -> saksbehandlerMedRoller.harRolleStrengtFortrolig()
            AdressebeskyttelseGradering.STRENGT_FORTROLIG -> saksbehandlerMedRoller.harRolleStrengtFortrolig()
            AdressebeskyttelseGradering.FORTROLIG -> saksbehandlerMedRoller.harRolleFortrolig()
            AdressebeskyttelseGradering.UGRADERT -> true
            else -> true
        }
}
