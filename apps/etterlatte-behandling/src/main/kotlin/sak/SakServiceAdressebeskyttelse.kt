package no.nav.etterlatte.sak

import no.nav.etterlatte.libs.common.pdlhendelse.AdressebeskyttelseGradering

interface SakServiceAdressebeskyttelse {
    fun behandlingHarAdressebeskyttelse(behandlingId: String): Boolean
    fun oppdaterAdressebeskyttelse(id: Long, adressebeskyttelseGradering: AdressebeskyttelseGradering): Int
    fun sjekkOmSakHarAdresseBeskyttelse(fnr: String): Boolean
}

class SakServiceAdressebeskyttelseImpl(private val dao: SakDaoAdressebeskyttelse) : SakServiceAdressebeskyttelse {

    private fun finnSakerForPerson(person: String) = dao.finnSaker(person)
    override fun sjekkOmSakHarAdresseBeskyttelse(fnr: String): Boolean {
        val sakIder = this.finnSakerForPerson(fnr).map { it.id }
        return dao.enAvSakeneHarAdresseBeskyttelse(sakIder)
    }
    override fun behandlingHarAdressebeskyttelse(behandlingId: String) =
        dao.sjekkOmBehandlingHarAdressebeskyttelse(behandlingId).harBeskyttelse()

    private fun AdressebeskyttelseGradering?.harBeskyttelse() = when (this) {
        AdressebeskyttelseGradering.FORTROLIG -> true
        AdressebeskyttelseGradering.STRENGT_FORTROLIG -> true
        AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND -> true
        else -> false
    }

    override fun oppdaterAdressebeskyttelse(id: Long, adressebeskyttelseGradering: AdressebeskyttelseGradering): Int {
        return dao.oppdaterAdresseBeskyttelse(id, adressebeskyttelseGradering)
    }
}