package no.nav.etterlatte.sak

import no.nav.etterlatte.SaksbehandlerMedRoller
import no.nav.etterlatte.libs.common.PersonTilgangsSjekk
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator

interface SakServiceAdressebeskyttelse : PersonTilgangsSjekk {
    fun harTilgangTilBehandling(behandlingId: String, saksbehandlerMedRoller: SaksbehandlerMedRoller): Boolean
    fun oppdaterAdressebeskyttelse(id: Long, adressebeskyttelseGradering: AdressebeskyttelseGradering): Int
    fun harTilgangTilSak(sakId: Long, saksbehandlerMedRoller: SaksbehandlerMedRoller): Boolean
    fun harTilgangTilPerson(fnr: String, saksbehandlerMedRoller: SaksbehandlerMedRoller): Boolean
}

data class SakMedGradering(val id: Long, val adressebeskyttelseGradering: AdressebeskyttelseGradering?)

class SakServiceAdressebeskyttelseImpl(
    private val dao: SakDaoAdressebeskyttelse,
    private val saksbehandlereGroupIdsByKey: Map<String, String>
) : SakServiceAdressebeskyttelse {

    override suspend fun harTilgangTilPerson(
        foedselsnummer: Folkeregisteridentifikator,
        bruker: no.nav.etterlatte.token.Saksbehandler
    ): Boolean {
        return this.harTilgangTilPerson(foedselsnummer.value, SaksbehandlerMedRoller(bruker))
    }

    override fun harTilgangTilPerson(fnr: String, saksbehandlerMedRoller: SaksbehandlerMedRoller): Boolean {
        val finnSakerMedGradering = dao.finnSakerMedGradering(fnr)
        if (finnSakerMedGradering.isEmpty()) {
            return true
        }
        val harRolleStrengtFortrolig = saksbehandlerMedRoller.harRolleStrengtFortrolig(saksbehandlereGroupIdsByKey)
        val harRolleFortrolig = saksbehandlerMedRoller.harRolleFortrolig(saksbehandlereGroupIdsByKey)

        return finnSakerMedGradering.map {
            when (it.adressebeskyttelseGradering) {
                AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND -> harRolleStrengtFortrolig
                AdressebeskyttelseGradering.STRENGT_FORTROLIG -> harRolleStrengtFortrolig
                AdressebeskyttelseGradering.FORTROLIG -> harRolleFortrolig
                AdressebeskyttelseGradering.UGRADERT -> true
                else -> true
            }
        }.all { it }
    }

    override fun harTilgangTilSak(sakId: Long, saksbehandlerMedRoller: SaksbehandlerMedRoller): Boolean {
        val harRolleStrengtFortrolig = saksbehandlerMedRoller.harRolleStrengtFortrolig(saksbehandlereGroupIdsByKey)
        val harRolleFortrolig = saksbehandlerMedRoller.harRolleFortrolig(saksbehandlereGroupIdsByKey)

        val sakMedGradering = dao.hentSakMedGradering(sakId) ?: return true
        return when (sakMedGradering.adressebeskyttelseGradering) {
            AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND -> harRolleStrengtFortrolig
            AdressebeskyttelseGradering.STRENGT_FORTROLIG -> harRolleStrengtFortrolig
            AdressebeskyttelseGradering.FORTROLIG -> harRolleFortrolig
            AdressebeskyttelseGradering.UGRADERT -> true
            else -> true
        }
    }

    override fun harTilgangTilBehandling(
        behandlingId: String,
        saksbehandlerMedRoller: SaksbehandlerMedRoller
    ): Boolean {
        return when (dao.sjekkOmBehandlingHarAdressebeskyttelse(behandlingId)) {
            AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND -> saksbehandlerMedRoller.harRolleStrengtFortrolig(
                saksbehandlereGroupIdsByKey
            )
            AdressebeskyttelseGradering.STRENGT_FORTROLIG -> saksbehandlerMedRoller.harRolleStrengtFortrolig(
                saksbehandlereGroupIdsByKey
            )
            AdressebeskyttelseGradering.FORTROLIG -> saksbehandlerMedRoller.harRolleFortrolig(
                saksbehandlereGroupIdsByKey
            )
            AdressebeskyttelseGradering.UGRADERT -> true
            else -> true
        }
    }

    override fun oppdaterAdressebeskyttelse(id: Long, adressebeskyttelseGradering: AdressebeskyttelseGradering): Int {
        return dao.oppdaterAdresseBeskyttelse(id, adressebeskyttelseGradering)
    }
}