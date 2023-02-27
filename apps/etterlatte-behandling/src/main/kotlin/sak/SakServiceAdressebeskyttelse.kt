package no.nav.etterlatte.sak

import no.nav.etterlatte.libs.common.pdlhendelse.AdressebeskyttelseGradering

interface SakServiceAdressebeskyttelse {
    fun behandlingHarAdressebeskyttelse(behandlingId: String): Boolean
}

class SakServiceAdressebeskyttelseImpl(private val dao: SakDaoAdressebeskyttelse) : SakServiceAdressebeskyttelse {

    override fun behandlingHarAdressebeskyttelse(behandlingId: String): Boolean {
        val adressebeskyttelseGradering = dao.sjekkOmBehandlingHarAdressebeskyttelse(behandlingId)
        return if (adressebeskyttelseGradering != null) {
            when (adressebeskyttelseGradering) {
                AdressebeskyttelseGradering.FORTROLIG -> true
                AdressebeskyttelseGradering.STRENGT_FORTROLIG -> true
                AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND -> true
                else -> false
            }
        } else {
            false
        }
    }
}