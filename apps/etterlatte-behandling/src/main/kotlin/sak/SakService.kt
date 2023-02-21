package no.nav.etterlatte.sak

import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.pdlhendelse.AdressebeskyttelseGradering

interface SakService {
    fun hentSaker(): List<Sak>
    fun finnSaker(person: String): List<Sak>
    fun finnEllerOpprettSak(person: String, type: SakType): Sak
    fun finnSak(person: String, type: SakType): Sak?
    fun finnSak(id: Long): Sak?
    fun slettSak(id: Long)
    fun oppdaterAdressebeskyttelse(id: Long, adressebeskyttelseGradering: AdressebeskyttelseGradering): Int
}

class RealSakService(private val dao: SakDao) : SakService {
    override fun hentSaker(): List<Sak> {
        return dao.hentSaker()
    }

    private fun finnSakerForPerson(person: String) = dao.finnSaker(person)

    override fun finnSaker(person: String): List<Sak> {
        return inTransaction {
            finnSakerForPerson(person)
        }
    }

    override fun slettSak(id: Long) {
        dao.slettSak(id)
    }

    override fun finnEllerOpprettSak(person: String, type: SakType): Sak {
        val eksisterendeSak = finnSak(person, type)
        return eksisterendeSak ?: dao.opprettSak(person, type)
    }

    override fun finnSak(person: String, type: SakType): Sak? {
        return finnSakerForPerson(person).find { it.sakType == type }
    }

    override fun finnSak(id: Long): Sak? {
        return dao.hentSak(id)
    }

    override fun oppdaterAdressebeskyttelse(id: Long, adressebeskyttelseGradering: AdressebeskyttelseGradering): Int {
        return dao.setAdresseBeskyttelse(id, adressebeskyttelseGradering)
    }
}