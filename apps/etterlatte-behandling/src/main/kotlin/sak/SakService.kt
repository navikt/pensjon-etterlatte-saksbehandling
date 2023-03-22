package no.nav.etterlatte.sak

import no.nav.etterlatte.behandling.klienter.Norg2Klient
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.klienter.PdlKlient
import no.nav.etterlatte.libs.common.behandling.SakType

interface SakService {
    fun hentSaker(): List<Sak>
    fun finnSaker(person: String): List<Sak>
    fun finnEllerOpprettSak(person: String, type: SakType): Sak
    fun finnSak(person: String, type: SakType): Sak?
    fun finnSak(id: Long): Sak?
    fun slettSak(id: Long)
    fun markerSakerMedSkjerming(sakIder: List<Long>, skjermet: Boolean)
    fun sjekkOmSakHarAdresseBeskyttelse(fnr: String): Boolean
    fun sjekkOmSakHarAdresseBeskyttelse(sakId: Long): Boolean
}

class RealSakService(private val dao: SakDao, private val pdlKlient: PdlKlient, private val norg2Klient: Norg2Klient) :
    SakService {

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

    override fun sjekkOmSakHarAdresseBeskyttelse(fnr: String): Boolean {
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

    private fun finnEnhet(person: String, tema: String) =
        pdlKlient.hentGeografiskTilknytning(person).geografiskTilknytning()?.let {
            norg2Klient.hentEnheterForOmraade(tema, it).firstOrNull()
        }

    override fun finnEllerOpprettSak(person: String, type: SakType) =
        finnSak(person, type) ?: dao.opprettSak(person, type, finnEnhet(person, type.tema)?.enhetNr)

    override fun finnSak(person: String, type: SakType): Sak? {
        return finnSakerForPerson(person).find { it.sakType == type }
    }

    override fun finnSak(id: Long): Sak? {
        return dao.hentSak(id)
    }
}