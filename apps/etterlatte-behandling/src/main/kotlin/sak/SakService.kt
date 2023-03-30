package no.nav.etterlatte.sak

import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.PersonTilgangsSjekk
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.token.Saksbehandler

interface SakService : PersonTilgangsSjekk {
    fun hentSaker(): List<Sak>
    fun finnSaker(person: String): List<Sak>
    fun finnEllerOpprettSak(person: String, type: SakType): Sak
    fun finnSak(person: String, type: SakType): Sak?
    fun finnSak(id: Long): Sak?
    fun slettSak(id: Long)
    fun markerSakerMedSkjerming(sakIder: List<Long>, skjermet: Boolean)
    fun sjekkOmFnrHarEnSakMedAdresseBeskyttelse(fnr: String): Boolean
    fun sjekkOmSakHarAdresseBeskyttelse(sakId: Long): Boolean
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

    override fun markerSakerMedSkjerming(sakIder: List<Long>, skjermet: Boolean) {
        inTransaction {
            dao.markerSakerMedSkjerming(sakIder, skjermet)
        }
    }

    override fun sjekkOmFnrHarEnSakMedAdresseBeskyttelse(fnr: String): Boolean {
        val sakIder = this.finnSaker(fnr).map { it.id }
        return inTransaction {
            dao.enAvSakeneHarAdresseBeskyttelse(sakIder)
        }
    }

    override fun sjekkOmSakHarAdresseBeskyttelse(sakId: Long): Boolean {
        return inTransaction {
            dao.enAvSakeneHarAdresseBeskyttelse(listOf(sakId))
        }
    }

    override suspend fun harTilgangTilPerson(
        folkeregisteridentifikator: Folkeregisteridentifikator,
        bruker: Saksbehandler
    ): Boolean {
        return !this.sjekkOmFnrHarEnSakMedAdresseBeskyttelse(folkeregisteridentifikator.value)
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
}