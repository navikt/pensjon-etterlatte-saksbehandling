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

    override fun finnEllerOpprettSak(person: String, type: SakType): Sak {
        val eksisterendeSak = finnSak(person, type)

        return if (eksisterendeSak != null) {
            eksisterendeSak
        } else {
            val sak = dao.opprettSak(person, type)

            val geografiskTilknytning = pdlKlient.hentGeografiskTilknytning(person)

            geografiskTilknytning.geografiskTilknytning()?.let { omraade ->
                val bestEnhet = norg2Klient.hentEnheterForOmraade(type.tema, omraade)

                bestEnhet.firstOrNull()?.let {
                    // TODO Update Sak med enhet
                }
            }

            sak
        }
    }

    override fun finnSak(person: String, type: SakType): Sak? {
        return finnSakerForPerson(person).find { it.sakType == type }
    }

    override fun finnSak(id: Long): Sak? {
        return dao.hentSak(id)
    }
}